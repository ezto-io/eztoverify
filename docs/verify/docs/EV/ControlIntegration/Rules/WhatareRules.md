---
id: WhatareRules
title: What are Rules?
sidebar_label: What are Rules?
---

**Rules** are conditions that define the specific circumstances under which a verification process should take place. They come into play only after a verification transaction has been triggered, specifying the exact criteria for the transaction to proceed. For instance, if a rule is set to require that the user must be over 18 years old, the verification process will handle it in different ways. If the condition is not met, the process will fail in [Gatepass](/docs/EV/ControlIntegration/ApplicationLevel) scenarios. However, in [Workflows,](/docs/EV/ControlIntegration/WorkflowsLevel) that particular flow will simply be skipped.

#### Key Aspects of Rules

- Rules determine whether a verification process needs to be initiated for a user. If a rule is satisfied, the transaction will be executed (for instance, in the case of [Gatepass](/docs/EV/ControlIntegration/ApplicationLevel)).  

- If the rule does not apply, the relevant verification will be skipped, initiating the process for users who meet the criteria.

- It can include conditions (such as time of day, IP address, or location) under which the verification is
**allowed or denied.**

Overall, rules are essential for establishing standardized, secure, and efficient processes that accurately verify individuals' identities.

#### For further assistance or related topics, you may explore the following resources

- [Create Rule](/docs/EV/ControlIntegration/Rules/CreateRule)
- [Application Level](/docs/EV/ControlIntegration/ApplicationLevel)
- [Workflows Level](/docs/EV/ControlIntegration/WorkflowsLevel)