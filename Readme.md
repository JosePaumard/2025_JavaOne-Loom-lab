JavaOne 2025 Loom Lab
=====================


You can find the slides for this lab here: https://speakerdeck.com/josepaumard/javaone25-loom-lab

## Introduction

The Loom project already brought a major feature in the JDK 21, and is developing two more features: Scoped Values and Structured Concurrency. This lab focuses on Structured Concurrency. It uses virtual threads transparently. 

It consists in the refactoring of a server application. You will start with an initial version that suffers from many problems. 

This lab takes you through a step by step refactoring, to parallelize the many requests it does, and to get a better latency and a more readable code.

You can follow it, or jump right to the step you would like to work on, using the different branches and labels of this repository. 

## Prerequisites

### The Loom Early Access Distribution

The features showcased in this lab are available on the JDK 24 Loom Early Access version that you can download here: https://jdk.java.net/loom/. The version used for this lab is the _Build 24-loom+10-110 (2024/11/6)_. So you need to download this version, and install it in your IDE. 

This Early Access version contains the version of the Structured Concurrency API that the Loom team is working on. It supersedes the current preview version you have in the JDK 24, and prefigures the evolution of this API in the JDK 25.  

### A Working Maven Installation

The application you will be working on is using the Helidon server (https://helidon.io/), as a Maven dependency. You do not need to download anything, Maven can take care of that for you. 

## Working on the Lab

All the instructions for the lab are in the file [JavaOne-Loom-Lab.md](JavaOne-Loom-Lab.md), in this directory. Once you have downloaded the Loom distribution you need and configured your project, you can start working on it. 

## References

Loom Early Access distibution: https://jdk.java.net/loom/
The Helidon page: https://helidon.io/

- JEP 444 Virtual Threads without Pinning: https://openjdk.org/jeps/444
- JEP 487 Scoped Values (Fourth Preview): https://openjdk.org/jeps/487
- JEP 491 Synchronize Virtual Threads without Pinning: https://openjdk.org/jeps/491
- JEP 499 Structured Concurrency (Fourth Preview): https://openjdk.org/jeps/499
- JEP 8340343 Structured Concurrency (Fifth Preview): https://openjdk.org/jeps/8340343
