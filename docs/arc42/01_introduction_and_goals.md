ATProtoFeedFramework is an open-source Java framework for building ATProto Feed Applications.

# Requirements Overview

The ATProto ecosystem enables developers to expose custom feeds through the standardized Feed Generator API. While implementing a custom feed is conceptually straightforward, production-ready deployments repeatedly require the same surrounding infrastructure, including indexing, persistence, runtime management, monitoring and deployment.

ATProtoFeedFramework addresses this recurring problem by providing a reusable Java framework that allows developers to focus on implementing feed logic rather than rebuilding infrastructure.

# Quality Goals

## Developer Experience

A developer should be able to implement a custom feed without understanding the complete internal architecture of the framework.

⸻

## Simplicity

The framework should remain understandable and easy to operate.

Avoid unnecessary abstractions.

⸻

## Maintainability

The code base should evolve through documented architectural decisions and stable terminology.

⸻

## Production Readiness

Applications built on the framework should integrate naturally into existing Java operational environments.

⸻

## Extensibility

New feed definitions should require minimal infrastructure code.

# Stakeholders

| Stakeholder              | Interest                                        |
|--------------------------|-------------------------------------------------|
| Java Developer           | Implement Feed Applications                     |
| Operators                | Stable deployment and maintenance               |
| Open Source Contributors | Clear architecture and contribution guidelines  |
| Framework Maintainers    | Sustainable evolution                           |
| Organizations            | Integrate ATProto into existing Java ecosystems |
