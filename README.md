---
page_type: sample
languages:
- java
products:
- azure
description: "Sample projects for developing and deploying Java applications with Open/WebSphere Liberty on an Azure Red Hat OpenShift 4 cluster."
urlFragment: "open-liberty-on-aro"
---

# Open/WebSphere Liberty on Azure Red Hat OpenShift Samples

<!-- 
Guidelines on README format: https://review.docs.microsoft.com/help/onboard/admin/samples/concepts/readme-template?branch=master

Guidance on onboarding samples to docs.microsoft.com/samples: https://review.docs.microsoft.com/help/onboard/admin/samples/process/onboarding?branch=master

Taxonomies for products and languages: https://review.docs.microsoft.com/new-hope/information-architecture/metadata/taxonomies?branch=master
-->

## Overview

[Azure Red Hat OpenShift](https://azure.microsoft.com/services/openshift/) provides a flexible, self-service deployment of fully managed OpenShift clusters. Maintain regulatory compliance and focus on your application development, while your master, infrastructure, and application nodes are patched, updated, and monitored by both Microsoft and Red Hat.

[Open Liberty](https://openliberty.io) is an IBM Open Source project that implements the Eclipse MicroProfile specifications and is also Java/Jakarta EE compatible. Open Liberty is fast to start up with a low memory footprint and supports live reloading for quick iterative development. It is simple to add and remove features from the latest versions of MicroProfile and Java/Jakarta EE. Zero migration lets you focus on what's important, not the APIs changing under you.

[WebSphere Liberty](https://www.ibm.com/cloud/websphere-liberty) architecture shares the same code base as the open sourced Open Liberty server runtime, which provides additional benefits such as low-cost experimentation, customization and seamless migration from open source to production.

This repository contains samples projects for developing and deploying Java applications with Open/WebSphere Liberty on an Azure Red Hat OpenShift 4 cluster.
These sample projects show how to use various features in Open/WebSphere Liberty and how to integrate with different Azure services.
Below table shows the list of samples available in this repository.

| Sample                           | Description                                | Guide                            |
|----------------------------------|--------------------------------------------|----------------------------------|
| [`1-start`](1-start) | Basic Java EE application with Java EE 8 (JAX-RS, EJB, CDI, JSON-B, JSF, Bean Validation). | |
| [`2-simple`](2-simple) | Migrate [`1-start`](1-start) sample to Open/WebSphere Liberty with minimum configurations. | [howto-guide](guides/howto-deploy-java-openliberty-app.md) |
| [`3-integration/elk-logging`](3-integration/elk-logging) | Extend [`2-simple`](2-simple) sample by integrating with Elasticsearch stack for distributed logging. | [howto-guide](guides/howto-integrate-elasticsearch-stack.md) |
| [`3-integration/connect-db`](3-integration/connect-db) | Extend [`2-simple`](2-simple) sample by integrating with Azure managed databases for data persistence. | [howto-guide](guides/howto-integrate-azure-managed-databases.md) |
| [`3-integration/aad-oidc`](3-integration/aad-oidc) | Extend [`2-simple`](2-simple) sample by integrating with Azure Active Directory OpenID Connect for security. | [howto-guide](guides/howto-integrate-aad-oidc.md) |
| [`3-integration/aad-ldap`](3-integration/aad-ldap) | Extend [`2-simple`](2-simple) sample by integrating with Azure Active Directory Domain Service via Secure LDAP for security. | [howto-guide](guides/howto-integrate-aad-ldap.md) |
| [`4-finish`](4-finish) | A complete sample with all services integration including security, data persistence & distributed logging. | [howto-guide](guides/howto-integrate-all.md) |

## Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
