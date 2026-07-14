# Glossary

This glossary defines the terminology used throughout the project documentation.

---

## ATProto

The open protocol powering Bluesky and related decentralized social networking services.

---

## Feed

A collection of posts selected according to a defined set of rules.

---

## Feed Application

A deployable application exposing one or more ATProto feeds through the standardized Feed Generator API.

---

## Feed Definition

A declarative description defining how a feed is composed.

A feed definition consists of one or more feed rules and optional ranking logic.

---

## Feed Rule

A rule determining whether a post belongs to a feed.

Multiple rules may contribute to a single feed definition.

---

## Feed Index

A persistent index storing references to posts that match one or more feed definitions.

The framework stores references only and does not archive post contents.

---

## Post Reference

A unique ATProto URI identifying a post.

The framework indexes post references instead of storing post contents.

---

## Runtime

The infrastructure responsible for executing feed definitions, maintaining indexes and exposing the Feed Generator API.

---

## Framework

The reusable infrastructure provided by ATProtoFeedFramework.

Applications built on the framework contribute feed definitions while the framework provides runtime, storage and protocol integration.

---

## Feed Generator API

The standardized ATProto interface used by feed applications.

Although the ATProto specification refers to this component as a "Feed Generator", it does not generate content. It exposes references to existing posts selected by the application's feed definitions.