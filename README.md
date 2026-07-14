# ATProtoFeedFramework

> Build feed logic, not feed infrastructure.

ATProtoFeedFramework is an open-source Java framework for building ATProto Feed Applications.

Instead of implementing the ATProto Feed Generator API from scratch for every project, developers can focus on defining feed rules while the framework provides the surrounding infrastructure, including indexing, storage, runtime management, observability and deployment support.

The framework is intended for developers and organizations that already operate Java-based environments and want to integrate ATProto feed applications into their existing development, deployment and operational workflows.

## About ATProto

ATProto is the open protocol powering Bluesky and related decentralized social networking services.

Among other extensibility mechanisms, ATProto allows developers to create custom feeds by implementing the Feed Generator API.

Although the ATProto specification refers to this component as a Feed Generator, it does not generate content. Instead, it selects and ranks existing posts and exposes them through a standardized API.

ATProtoFeedFramework provides a Java-based foundation for building and operating these Feed Applications.

## Why does this project exist?

The ATProto ecosystem provides a powerful protocol for creating custom feeds. Existing implementations are predominantly based on JavaScript and TypeScript, which are an excellent fit for many use cases.

However, many organizations and developers already maintain mature Java ecosystems with established build pipelines, operational practices and long-term maintenance strategies. Rebuilding the same infrastructure in a different technology stack introduces unnecessary operational complexity.

ATProtoFeedFramework aims to provide a familiar, production-oriented Java foundation for building and operating ATProto Feed Applications. Instead of repeatedly solving infrastructure concerns such as indexing, persistence, configuration, monitoring and deployment, developers can focus on the one aspect that makes every feed unique:

**its selection and ranking logic.**

## Goals

The project pursues a small number of clearly defined goals:

- provide reusable infrastructure for ATProto Feed Applications
- reduce the amount of boilerplate required to implement custom feeds
- enable developers to focus on feed logic instead of protocol details
- integrate naturally into modern Java development environments
- support production-ready deployments
- keep operational complexity as low as reasonably possible

## Non Goals

ATProtoFeedFramework intentionally does **not** aim to become:

- a Personal Data Server (PDS)
- an AppView implementation
- a search engine
- a content archive
- a replacement for the ATProto protocol
- a complete social networking platform

## Current Status

The project is currently in the architectural design phase.

The initial focus is on defining a clean architecture, stable terminology and a sustainable extension model before implementing production code.

## Contributing

The project is currently under active development.

Contributions, discussions and constructive feedback are welcome once the initial architecture has been established.

## License

The license will be defined before the first public release.

## Project Documentation

Project documentation is available in the [docs](docs) folder.