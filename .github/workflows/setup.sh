#!/usr/bin/env bash
################################################
# This script is invoked by a human who:
# - has done az login.
# - can create repository secrets in the github repo from which this file was cloned.
# - has the gh client >= 2.0.0 installed.
#
# This script initializes the repo from which this file is was cloned
# with the necessary secrets to run the workflows.
#
# Script design taken from https://github.com/microsoft/NubesGen.
#
################################################

################################################
# Set environment variables - the main variables you might want to configure.
#
# Three letters to disambiguate names.
DISAMBIG_PREFIX=
# The location of the resource group. For example `eastus`. Leave blank to use your default location.
LOCATION=
SLEEP_VALUE=30s

# End set environment variables
################################################


set -Eeuo pipefail
trap cleanup SIGINT SIGTERM ERR EXIT

cleanup() {
  trap - SIGINT SIGTERM ERR EXIT
  # script cleanup here
}

setup_colors() {
  if [[ -t 2 ]] && [[ -z "${NO_COLOR-}" ]] && [[ "${TERM-}" != "dumb" ]]; then
    NOFORMAT='\033[0m' RED='\033[0;31m' GREEN='\033[0;32m' ORANGE='\033[0;33m' BLUE='\033[0;34m' PURPLE='\033[0;35m' CYAN='\033[0;36m' YELLOW='\033[1;33m'
  else
    NOFORMAT='' RED='' GREEN='' ORANGE='' BLUE='' PURPLE='' CYAN='' YELLOW=''
  fi
}

msg() {
  echo >&2 -e "${1-}"
}

setup_colors

read -r -p "Enter password OpenShift userid: " ARO_PASSWORD
read -r -p "Enter admin user for database: " DB_ADMIN_USER
read -r -p "Paste password for database: " DB_PASSWORD
read -r -p "Paste database server name: " DB_SERVER_NAME
read -r -p "Enter an at least three letter long disambiguation prefix (try your initials): " DISAMBIG_PREFIX
read -r -p "Enter owner/reponame of GitHub repository (blank for upsteam of current fork): " OWNER_REPONAME

if [ "$DISAMBIG_PREFIX" == '' ] ; then
  msg "${RED}You must enter a disambiguation prefix."
  exit 1;
fi

if [ -z "${OWNER_REPONAME}" ] ; then
    GH_FLAGS=""
else
    GH_FLAGS="--repo ${OWNER_REPONAME}"
fi

DISAMBIG_PREFIX=${DISAMBIG_PREFIX}`date +%m%d`
SERVICE_PRINCIPAL_NAME=${DISAMBIG_PREFIX}sp
USER_ASSIGNED_MANAGED_IDENTITY_NAME=${DISAMBIG_PREFIX}u
STORAGE_ACCOUNT=${DISAMBIG_PREFIX}st

# get default location if not set at the beginning of this file
if [ "$LOCATION" == '' ] ; then
    {
      az config get defaults.location --only-show-errors > /dev/null 2>&1
      LOCATION_DEFAULTS_SETUP=$?
    } || {
      LOCATION_DEFAULTS_SETUP=0
    }
    # if no default location is set, fallback to "eastus"
    if [ "$LOCATION_DEFAULTS_SETUP" -eq 1 ]; then
      LOCATION=eastus
    else
      LOCATION=$(az config get defaults.location --only-show-errors | jq -r .value)
    fi
fi

# Check AZ CLI status
msg "${GREEN}(1/9) Checking Azure CLI status...${NOFORMAT}"
{
  az > /dev/null
} || {
  msg "${RED}Azure CLI is not installed."
  msg "${GREEN}Go to https://aka.ms/nubesgen-install-az-cli to install Azure CLI."
  exit 1;
}
{
  az account show > /dev/null
} || {
  msg "${RED}You are not authenticated with Azure CLI."
  msg "${GREEN}Run \"az login\" to authenticate."
  exit 1;
}

msg "${YELLOW}Azure CLI is installed and configured!"

# Check GitHub CLI status. Use gh auth login to login
msg "${GREEN}(2/9) Checking GitHub CLI status...${NOFORMAT}"
USE_GITHUB_CLI=false
{
  gh auth status && USE_GITHUB_CLI=true && msg "${YELLOW}GitHub CLI is installed and configured!"
} || {
  msg "${YELLOW}Cannot use the GitHub CLI. ${GREEN}No worries! ${YELLOW}We'll set up the GitHub secrets manually."
  USE_GITHUB_CLI=false
}

# Execute commands

### AZ ACTION CREATE

msg "${GREEN}(3/9) Create storage account and container ${STORAGE_ACCOUNT}"

az group create --name ${STORAGE_ACCOUNT} --location ${LOCATION} -o none
az storage account create --resource-group ${STORAGE_ACCOUNT} --name ${STORAGE_ACCOUNT} --sku Standard_LRS --allow-blob-public-access false --encryption-services blob -o none

msg "${GREEN}(4/9) Get storage account key"
ACCOUNT_KEY=$(az storage account keys list --resource-group ${STORAGE_ACCOUNT} --account-name ${STORAGE_ACCOUNT} --query '[0].value' -o tsv)
msg "${GREEN}(5/8) Create blob container"
az storage container create --name ${STORAGE_ACCOUNT} --account-name ${STORAGE_ACCOUNT} --account-key $ACCOUNT_KEY -o none

msg "${GREEN}(6/9) Create service principal and Azure credentials ${SERVICE_PRINCIPAL_NAME}"
SUBSCRIPTION_ID=$(az account show --query id --output tsv --only-show-errors)

### AZ ACTION CREATE

SERVICE_PRINCIPAL=$(az ad sp create-for-rbac --name ${SERVICE_PRINCIPAL_NAME} --role="Contributor" --scopes="/subscriptions/${SUBSCRIPTION_ID}" --sdk-auth --only-show-errors | base64 -w0)
AZURE_CREDENTIALS=$(echo $SERVICE_PRINCIPAL | base64 -d)

### AZ ACTION CREATE

msg "${GREEN}(7/9) Create User assigned managed identity ${USER_ASSIGNED_MANAGED_IDENTITY_NAME}"
az group create --name ${USER_ASSIGNED_MANAGED_IDENTITY_NAME} --location ${LOCATION}
az identity create --name ${USER_ASSIGNED_MANAGED_IDENTITY_NAME} --location ${LOCATION} --resource-group ${USER_ASSIGNED_MANAGED_IDENTITY_NAME} --subscription ${SUBSCRIPTION_ID}
USER_ASSIGNED_MANAGED_IDENTITY_ID_NOT_ESCAPED=$(az identity show --name ${USER_ASSIGNED_MANAGED_IDENTITY_NAME} --resource-group ${USER_ASSIGNED_MANAGED_IDENTITY_NAME} --query id)

### AZ ACTION MUTATE

msg "${GREEN}(8/9) Grant Contributor role in subscription scope to ${USER_ASSIGNED_MANAGED_IDENTITY_NAME}. Sleeping for ${SLEEP_VALUE} first."
sleep ${SLEEP_VALUE}
ASSIGNEE_OBJECT_ID=$(az identity show --name ${USER_ASSIGNED_MANAGED_IDENTITY_NAME} --resource-group ${USER_ASSIGNED_MANAGED_IDENTITY_NAME} --query principalId)
# strip quotes
ASSIGNEE_OBJECT_ID=${ASSIGNEE_OBJECT_ID//\"/}
az role assignment create --role Contributor --assignee-principal-type ServicePrincipal --assignee-object-id ${ASSIGNEE_OBJECT_ID} --subscription ${SUBSCRIPTION_ID} --scope /subscriptions/${SUBSCRIPTION_ID}

msg "${GREEN}(9/9) Create secrets in GitHub"
if $USE_GITHUB_CLI; then
  {
    msg "${GREEN}Using the GitHub CLI to set secrets.${NOFORMAT}"
    gh ${GH_FLAGS} secret set ARO_PASSWORD -b"${ARO_PASSWORD}"    
    gh ${GH_FLAGS} secret set AZURE_CREDENTIALS -b"${AZURE_CREDENTIALS}"
    gh ${GH_FLAGS} secret set DB_ADMIN_USER -b"${DB_ADMIN_USER}"
    gh ${GH_FLAGS} secret set DB_PASSWORD -b"${DB_PASSWORD}"
    gh ${GH_FLAGS} secret set DB_SERVER_NAME -b"${DB_SERVER_NAME}"
  } || {
    USE_GITHUB_CLI=false
  }
fi
if [ $USE_GITHUB_CLI == false ]; then
  msg "${NOFORMAT}======================MANUAL SETUP======================================"
  msg "${GREEN}Using your Web browser to set up secrets..."
  msg "${NOFORMAT}Go to the GitHub repository you want to configure."
  msg "${NOFORMAT}In the \"settings\", go to the \"secrets\" tab and the following secrets:"
  msg "(in ${YELLOW}yellow the secret name and${NOFORMAT} in ${GREEN}green the secret value)"
  msg "${YELLOW}\"ARO_PASSWORD\""
  msg "${GREEN}${ARO_PASSWORD}"
  msg "${YELLOW}\"AZURE_CREDENTIALS\""
  msg "${GREEN}${AZURE_CREDENTIALS}"
  msg "${YELLOW}\"DB_ADMIN_USER\""
  msg "${GREEN}${DB_ADMIN_USER}"
  msg "${YELLOW}\"DB_PASSWORD\""
  msg "${GREEN}${DB_PASSWORD}"
  msg "${YELLOW}\"DB_SERVER_NAME\""
  msg "${GREEN}${DB_SERVER_NAME}"
  msg "${NOFORMAT}========================================================================"
fi
msg "${GREEN}Secrets configured"
