# 1. Introduction

**Team Nexus B · Group 29 · DCIT 204/308 Joint Data Structures & Algorithms
Project**, University of Ghana, 26 July – 22 August 2026.

## What was built

A service-operations platform for a synthetic Greater Accra health facility
network: it triages incoming cases, dispatches ambulances and response
teams over a weighted road graph, indexes case records for fast lookup, and
measures its own algorithmic performance. Fifteen members split across five
sub-teams, one package each (`data/`, `linear/`, `trees/`, `graphs/`,
`algorithms/`+`bench/`), coordinated through a shared `core/` package of
frozen interfaces and an `app/` layer that wires the pieces together.

## Why it exists

The brief's core requirement is that every data structure and algorithm in
the system be implemented from scratch — no `java.util` collections
anywhere in `src/main/java/edu/ug/nexusb` outside the `data/` and `bench/`
packages, which are exempted for file parsing, database plumbing, and
benchmark scaffolding rather than core algorithmic logic. That constraint
is enforced by a CI check on every push
(`.github/workflows/build.yml`), not just a style guideline: a forgotten
`import java.util.ArrayList` fails the build.

The problem domain — dispatch, triage, routing, records — was chosen
because it naturally needs almost every structure and algorithm the course
covers, and because each one maps to a concrete, motivated use case rather
than an abstract exercise: a priority queue for triage isn't a demo of
heaps, it's how an emergency case actually jumps the line ahead of a
routine one.

## Scope of this document

Sections 2-5 cover what the system does and how it's built; sections 6-8
cover the algorithms, their correctness/complexity evidence, and the
benchmark results measuring them; section 9 is an honest accounting of
trade-offs and known limitations; sections 10-11 close with individual
contributions and a conclusion, written last, once the rest is settled.
