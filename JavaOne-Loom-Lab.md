JavaOne 2025 Loom Lab: Refactoring to Structured Concurrency
============================================================

## Table of Contents
1. [Introduction](#introduction)
2. [Presenting the Application](#presenting-the-application)
3. [Running the Application](#running-the-application)
4. [Goal of the Refactoring](#goal-of-the-refactoring)
5. [Using the Structured Concurrency API](#using-the-structured-concurrency-api)
6. [Preparing the Code for the Structured Concurrency API](#preparing-the-code-for-the-structured-concurrency-api)
7. [Using the Structured Concurrency API to Query a Travel](#using-the-structured-concurrency-api-to-query-a-travel)
8. [Updating the StructuredTaskScope Strategy to Manage Exceptions](#updating-the-structuredtaskscope-strategy-to-manage-exceptions)
9. [Using the Structured Concurrency API to Query the Weather Forecast](#using-the-structured-concurrency-api-to-query-the-weather-forecast)
10. [Using the Structured Concurrency API to Query a Travel in Parallel](#using-the-structured-concurrency-api-to-query-a-travel-in-parallel)


## Introduction

This lab takes you through the refactoring of an application using the Structured Concurrency API. 

This application is a server application for a travel agency. This agency sells flights, and would like to show the weather forecast of the destination, as an incentive for the selling of their travels. 

This application has to connect to other services, held by air companies, and weather forecast agencies. The travel page needs to display two elements. 

1. The cheapest flight. This information is mandatory. 
2. The weather forecast at the destination. This information is optional. 

The Structured Concurrency API is developed under the Loom project umbrella, that brought Virtual Threads to the Java platform. As of this writing, this API is still a preview feature of the JDK. 

The goal of this lab is to show you how this API is working, and how you can use it to get the best performance in your server applications, while preserving the readability of your code, a point where callback-based reactive programming fails.

At the end of this lab, you will have a precise idea on how the Structured Concurrency API is working, how you can open and use the instances of the `StructuredTaskScope` interface that the JDK gives you. You will see how you can submit `Callable` objects to a `StructuredTaskScope`, and how you can analyze the result you get. You will know what strategies `StructuredTaskScope` instances use, how you can select one or another using the `Joiner` interface, and how you can use them.  

This lab is using the Helidon server. Of course, everything is executed locally. 

[<< Return to top](#table-of-contents)

## Presenting the Application

The application is divided in 3 modules. 

1. The `A_Weather-server` module. In the scenario we are working on, this module represents an external service. You are not supposed to be able to modify this code. This server gives you the weather forecast in a given city. Note that it is really an over simplified version of a real weather forecast server, that returns a random result. It does not depend on any date, which is weird, and takes a city as an argument, which is actually not used.   
2. The `B_Companies-server` module. Same as for the previous module: this is a server that you are not supposed to touch. It can tell you if a given company has some flights between two cities, and at what price. If a company cannot offer you a direct flight, it can try to offer a multileg flight. 
3. The `C_Travel-agency-server` module, which is the module you are going to work on. 

In a real world scenario, these first two modules would be a collection of servers: one for each weather forecast agency, and one for each flight company. To make things simpler, there is one server for each group of weather agencies on the one hand, and flight companies on the other hand. 

### The A_Weather-server Module

This is the module that provides the weather forecast your travel agency needs. You can run it by running the `main` method in the `WeatherServer` class. It launches a Helidon web server with several elements.

This Helidon server uses two parameters: a host and a port. Both are read in the [server.properties](server.properties) file, at the root of this project. You may need to adapt these values to the machine you are running this lab on.

```text
# host and port for the weather forecast server
weather-agencies.host=127.0.0.1
weather-agencies.port=8080
```

Once you started the server, you can open a page http://127.0.0.1:8080. This page is very simple, if you need to modify it, it is located here: `A_Weather-server/src/main/resources/static-content/index.html`.

The last 3 links of this page are the following.

1. `Who am I?` It shows the name of the thread that Helidon is using to serve your request. This name looks like: `VirtualThread[#45,[...] WebServer socket]/runnable@ForkJoinPool-1-worker-3`. As you can see it is a virtual thread.
2. `Cities` gives you the list of the cities supported by the server. This list is located in the file `files/us-cities.txt`. You can add more cities to this file, but be careful, because you will need to the `flights-and-prices.txt` file if you do so. More on this point later.
3. `Companies` gives you the list of the weather forecast services created by this server. This list is located in the file `files/weather-agencies.txt`. Its content is the following:

```text
# Name, tag, average response time in ms, dispersion
Global Weather, global, 100, 20
Star Weather, star, 80, 10
Planet Weather, planet, 90, 25
```

The lines that start with a `#` are comments.

Then each line describes a single weather forecast service.

- `name`: the name of the weather forecast agency.
- `tag`: its tag. This tag cannot contain any blank characters, it is used to build the URL for the service of this agency.
- `average response time in ms` and `dispersion` are the parameters used to create a delay for the response provided by this service. This delay is a random number drawn between  `average - dispersion` and `average + dispersion`. It is applied with a call to `Thread.sleep()`.

Then you can click on the `weather.html` link, which takes you to the page that can query the services. You just need to select a city, then click on the agency service you want to query. The result is then displayed.

This result is random, so querying the same service for the same city will give you different results. There is no error handling in this application, so you will get a result even if you do not select any city.

### The B_Companies-server Module

This module gives you the price for a given flight and a given company, if this flight exists. You can run it by running the `main` method in the `CompanyServer` class. It launches a Helidon web server with several elements.

As for the weather forecast server, the Helidon server uses two parameters: a host and a port, read from the [server.properties](server.properties) file, at the root of this project.

You may need to adapt these values to the machine you are running this lab on.

```text
# host and port for the company server
companies.host=127.0.0.1
companies.port=8088
```

Once you started the server, you can open a page http://127.0.0.1:8088 (note that the port is different from the one used by the Weather forecast server). This page is very simple. It is located here should you want to modify it: `B_Companies-server/src/main/resources/static-content/index.html`.

The last 3 links of this page are the same as the ones on the Weather forecast server.

1. `Who am I?` gives you the name of the thread that Helidon is using to serve your request. This name looks like: `VirtualThread[#45,[...] WebServer socket]/runnable@ForkJoinPool-1-worker-3`. As you can see it is a virtual thread.
2. `Cities` gives you the list of the cities supported by the server. This server uses the same list, which located in the file `files/us-cities.txt`.
3. `Companies` gives you the list of the companies for which each service is created by this server. This list is located in the file `files/companies.txt`.
4. `travel.html` takes you to the page you can use to query the flight companies server.

#### Reading the Flight Companies

The companies are read from the `files/companies.txt` file. Its content is the following.

```text
# Name, pricing strategy, flight availability rate, tag, average response time in ms, dispersion
Air Penguin 90 70 air-penguin 100 20
Norwegian Parrots 100 80 norwegian-parrots 90 20
Gamma Airlines 110 90 gamma 80 20
Crusty Albatross 100 95 crusty-albatros 80 10
Diamond Airline 120 100 diamond 70 20
```

The lines that start with a `#` are comments.

Then each line describes a single flight company service.

- `name`: the name of the company
- `tag`: its tag. This tag cannot contain any blank characters, it is used to build the URL for the service of this company.
- `average response time in ms` and `dispersion` are the parameters used to create a delay for the response provided by this service. This delay is a random number drawn between  `average - dispersion` and `average + dispersion`. It is applied with a call to `Thread.sleep()`.
- `pricing strategy` is a parameter used to set the price for a given flight. Each flight between two city has a nominal price. The final price for a company is a percentage of the nominal price. This percentage is a random number between the pricing strategy plus or minus 10. The computation is slightly different for multileg flights: the percentage is divided by 1.25 in that case. You can see how these prices are computed in the two `Company.price()` methods.
- `flight availability rate` is a percentage used when the building the list of the cities where the given company operates. When the application is launched, the list of these cities is randomly computed. You can see this point in the `Companies.ofCompany()` method.

#### Reading the Flights

The flights and their prices are read from the file `files/flights-and-prices.txt`. Here is a small portion of this file.

```text
# Flights and Prices
     1   2   3   4   5
 1   -
 2 236   -
 3 185 147   -
 4 166   - 213   -
 5 143   - 389   -   -
```

As for the other files, the lines that start with a `#` are comments.

The first line contains the index of each city in the `files/cities.txt` file. Here is the relevant content of this file.

```text
# Cities
 1 Atlanta
 2 Austin
 3 Boston
 4 Chicago
 5 Dallas
```

Each of the following lines starts with the index of a city, and then a series of numbers or `-`. This number is the nominal price of a flight between these two cities. If there is `-`, it means that no company has any flight between these two cities.

Obviously there is no flight from a city to itself, so there is always a `-` on the diagonal. We suppose that the price of a flight between A and B is the same as the price between B and A, so only the lower half of the table needs values.

You can see that the nominal price of a flight between Boston and Austin is 147, and 185 between Atlanta and Boston. There is no flight between Austin and Chicago, or between Chicago and Dallas.

If you want to add cities to the `files/us-cities.txt` file, then you also need to add the connections and prices between the new cities and the existing ones.

#### Querying the Flight Companies Server

Clicking on the http://127.0.0.1:8088/travel.html page takes you to the Flight Companies page.

Here you can select a city of departure, and a city of destination. Then, if you click on one of the company button, you will see the result.

This result can be one of these three.

1. The given company cannot take you between these two cities. This is usually the case if you select a flight between Miami and Denver, and query the Air Penguin company. There is some randomness in this choice, so it may vary.
2. The given company has a direct flight, and gives you the price of this flight. You can select a flight between Atlanta and San Francisco, and query the Gamma Airline company. Note that, as the price is random, querying the same flight several time with the same company will give you different results.
3. The given company has no direct flight, but offers a multileg flight. If you select a flight from Las Vegas to Denver and query Diamond Airlines, odds are that you will see a multileg flight.

### The C_Travel-agency-server Module

Now that you have launched both the Weather Forecast server and the Companies server, you can launch the application you are going to work on. You can run the `main` method from the `TravelAgencyServer` class.

Again, you can specify the host and the port for this server in the [server.properties](server.properties) file.

```text
# host and port for the travel agency server
travel-agency.host=127.0.0.1
travel-agency.port=8090
```

You can then connect to the http://127.0.0.1:8090/travelpage.html page to see the application.

This application looks like the previous one: you can choose a city of departure and a city of destination, then click on the `Find a travel` button to get the best price among the different companies.

It may take a very long time for the result to be displayed. As much as several seconds, if not tens of seconds. But in the end, you should have a response: a price and a weather forecast at destination.

You can try to query for a flight from Atlanta to San Francisco. You will probably get a direct flight. If you query a flight from Miami to Philadelphia, you will probably get a multileg flight.

With a response time that is so high, there is little chance that your travel agency has any success online, something needs to be done!

[<< Return to top](#table-of-contents)

## Running the Application

The application you are working on is in the `C_Travel-agency-server` module. To be able to use this application correctly, you first need to run the applications in the `A_Weather-server` module and the `B_Companies-server` module. 

This is the procedure you should be following. 

1. Run the `org.paumard.server.weather.WeatherServer`[WeatherServer](A_Weather-server/src/main/java/org/paumard/server/weather/WeatherServer.java) class from the `A_Weather-server` module. 
2. Run the [CompanyServer](B_Companies-server/src/main/java/org/paumard/server/company/model/CompanyServer.java) class from the `B_Companies-server` module
3. Run the [TravelAgencyServer](C_Travel-agency-server/src/main/java/org/paumard/server/travel/TravelAgencyServer.java) class from the `C_Travel-agency-server` module. 

Note that the [TravelAgencyServer](C_Travel-agency-server/src/main/java/org/paumard/server/travel/TravelAgencyServer.java) class will try to connect to the servers launched by the [WeatherServer](A_Weather-server/src/main/java/org/paumard/server/weather/WeatherServer.java) class and the [CompanyServer](B_Companies-server/src/main/java/org/paumard/server/company/model/CompanyServer.java) class. If it cannot, you will not get the results you would expect on the travel page. You need to make sure that the hosts and ports that you set in the [server.properties](server.properties) file are correct. 

[<< Return to top](#table-of-contents)

## Goal of the Refactoring

The goal of the refactoring is to get a decent latency on your page. Let us examine how the response is built. 

### Navigating in the Code

Navigating in a code base is not something easy to do. There are `// FIXME` lines in the code, so that this guide can refer to them. Usually these comments are handled in a special way by IDEs, and you can see them in color in one of the lateral bars. 

### Analyzing the TravelAgencyServer Class

Everything is happening in the `TravelAgencyServer` class and in the `main` method. The code is written following the well-known and universally acclaimed spaghetti pattern (who doesn't love spaghetti?). The core of the code is creating the handler that manages your request. It starts on the line `// FIXME: Handler start` , with the construction of the travel handler, until the line `// FIXME: Handler end`. That is more than 75 lines and code to fix. 

It starts with a for loop (line `// FIXME: Company for loop`), which queries the Companies server for each company, one at a time. A first possibility to reduce the latency would be to launch these requests in parallel. With 5 companies, we could save a lot of latency by doing this. And even more if you decide to add more companies. 

If there is a flight available (line `// FIXME: best flight`), it then does the same with the different weather forecast companies (lines `// FIXME: weather agencies for loop`). With 3 weather forecast companies, this is another place where we could save some time. 

A first step of the refactoring could be to query all these services in parallel instead of querying them one at a time. And this is exactly a job for the Structured Concurrency API. That been said, the approach for the flight prices is not the same as for the weather forecast. You may want to limit the time it takes for a flight company service to give you a response, but you still prefer to have several responses. For the weather forecast, as they are supposed to be the same, all you need is the first response. Once you have it you can display it without having to wait for the others. This is again something that the Structured Concurrency API can do. You could even want to limit the global time with another strategy. You can wait for the weather forecast until you can send a price to your user. If you don't have the weather forecast, then you can skip it and choose not to display anything. 

[<< Return to top](#table-of-contents)

## Using the Structured Concurrency API

### Writing Simple Queries With the Structured Concurrency API

The Structured Concurrency API is the API you need when you want to launch network requests in parallel in virtual threads. In a nutshell: you create an instance of a class called `StructuredTaskScope`, you submit tasks to it, and you get the result of your tasks through a `Future`-like object. 

Described in that way, it really looks like an `ExecutorService`, and indeed it does. But it does many more things, that you are going to see. 

And because it only uses virtual threads, you can create them on demand, and close them when you do not need them anymore. The executor services that your application is using are most probably creating platform threads, meaning that you need to create them when your application starts, and that you need to keep them alive for as long as you can, so that you can reuse them. A classical `ExecutorService` is an expensive object to create, because platform threads are themselves expensive to create. You cannot create them on demand, and letting them die would be a performance hit in your application. This is the case for all the classical instances that you can build with the `Executors` factory class. There is one though that creates virtual thread instead of platform threads, which is the `Executors.newVirtualThreadPerTaskExecutor()`. 

Your basic Structured Concurrency pattern looks like the following. 

```java
// Create a StructuredTaskScope object in a try-with-resources statement
try (var scope = StructuredTaskScope.open()) { 
    
    // Submit tasks to it: these are Callable
    // StructuredTaskScope also supports Runnable
    // What you get in return in a Subtask object
    Subtask<String> future1 = scope.fork(callable1);
    Subtask<Integer> future2 = scope.fork(callable1);
    Subtask<Void> future3 = scope.fork(runnable);

    // Call join
    scope.join();   // Join subtasks, propagating exceptions

    // Both subtasks have succeeded, so compose their results
    return new Response(future1.get(), future2.get());
}
```

When you are done using your `StructuredTaskScope` object, the try-with-resources statement just closes it, and it will clean up all the resources it created, including any other `StructuredTaskScope` that your tasks may have created, thanks to the use of the `AutoCloseable` feature. 

Note that in this code, the `fork()` calls immediately return with a `Subtask` object. These are not blocking calls. The call to `scope.join()` on the other hand is a blocking call. It waits for all the tasks you submit to return, either with a result, or an exception. 

One of the things you need to do to use the Structured Concurrency API is to refactor your code so that you can extract your blocking requests to callables or runnables, and get well identified objects as result. The code you have at the moment is not like that, so you need to reorganize it. 

This lab shows you how to do it, following the principles of Data Oriented Programming, so that you can see the benefits in using this approach. 

[<< Return to top](#table-of-contents)

## Preparing the Code for the Structured Concurrency API

This part starts on the branch `Step-00_Initial-application`. There is also a tag of the same name, to keep a pointer on the right commit in case you want to add some code in this branch. 

First thing first, you need to clean up this spaghetti code, then clearly identify the tasks that you are launching, and make them instances of `Callable`.

The code in the `TravelHandler.travelHandler()` method does two things: first it queries the company servers for the flights, and then the weather forecast servers for the weather forecast.


### Extracting the Flight Request

Let us clean this code a little. There are simple things you can do to make it more readable. 

Let us extract the following code to a method.

```java
// FIXME: Extracting the requested flight 
var travelRequest = req.content().as(TravelRequestDTO.class);
var cityFrom = Cities.byName(travelRequest.from());
var destinationCity = Cities.byName(travelRequest.to());
var flight = Flight.from(cityFrom).to(destinationCity);
```

The `flight` object contains the destination city, so you can get it from there. This code becomes the following.

```java
// FIXME: Extracting the requested flight
var flight = queriedFlightFrom(req);
var destinationCity = flight.to();
```

Here is the code of the method `queriedFlightFrom()` method.

```java
static Flight queriedFlightFrom(ServerRequest request) {
    var travelRequest = request.content().as(TravelRequestDTO.class);
    var cityFrom = Cities.byName(travelRequest.from());
    var destinationCity = Cities.byName(travelRequest.to());
    var flight = Flight.from(cityFrom).to(destinationCity);
    return flight;
}
```

By the way, this `TravelRequestDTO` record is useless as it is. It is just there to model the Json you get. So you can define a record inside the `TravelAgencyServer` server class, and get rid of this `TravelRequestDTO` class. You can use this `TravelRequest` record where the  `TravelRequestDTO` record is used. There are other good candidates to be removed from this application in this `dto` package, we will take care of them later.

```java
public class TravelAgencyServer {
    public record TravelRequest(String from, String to) {}
    
    // rest of the class
}
```

### Refactoring the Querying of the Company Servers

Let us work on the content of this `// FIXME: Company for loop` for loop. 

This code queries the company server, and analyze its response. There are four outcomes for this query.  

1. You got a response, and it contains a simple flight with a price. 
2. You got a response, and it contains a multileg flight with a price. 
3. You could query the company servers, but for some reason the response you get does not contain any information about the flight you can sell. 
4. Something went seriously wrong, the request just failed, and nothing can be done. 

You can structure your code following the principles of Data Oriented Programming, which leads to a code that is easy to understand and to maintain. 

What you want when using such an approach, is to design your records so that you can deconstruct them and get the information you need from them. Your records are built from the Json you get, but you want to be able to get the flight and the price when you deconstruct them. This is what you need to have in mind when you design your records: you need factory methods to build them with what you have (here, Json objects), and to choose their components with the deconstruction in mind.


Following this pattern, your code will have the following structure. 

```java
try (var response = WebClient.builder()
        .baseUri(clientURI).build()
        .post("/company/" + company.tag())
        .submit(flight);
     ) {
    var companyServerResponse = CompanyServerResponse.of(response);
    return switch (companyServerResponse) {
        case SimpleFlight(SimpleFlight simpleFlight, int price) ->
            // return something
        case MultilegFlight(MultilegFlight multilegFlight, int price) ->
            // return something
        case NoFlight(String message) ->
            // return something
        case Error(String message) ->
            // return something
    };
}
```

To do that, you can create a sealed interface, `CompanyServerResponse`, with as many implementing records as you have cases. 

The records that implement this interface reference two types of flight: simple flights and multileg flights. There are also good candidates to be part of a sealed hierarchy. 

#### Creating a Flight Sealed Interface

So let us first create a `Flight` sealed interface in the `flight` package. You can then create the record you needs as member of this interface. In that case you do not need to add a `permit` clause to your sealed interface.

The first record that implements this interface is the `SimpleFlight` that you need. The code of this record is the same as the existing `Flight` record. So you can just copy and paste it there. You need to be careful by doing so: your IDE may want to still want to refer elements from your original `Flight` record. Remember also that this record should not reference `Travel` anymore, nor `Flight`, but `SimpleFlight` instead. 

The second record that implements this interface is the `MultilegFlight` record. You can also copy the code from the existing `MultilegFlight` record, with the same precautions as for the `Flight` record. It should not reference `Travel` anymore, nor `Flight`, but `SimpleFlight` instead.

Your `Flight` sealed interface should look like the following. 

```java
public sealed interface Flight {
    
    record SimpleFlight(City from, City to) 
            implements Flight {
        // content from the existing Flight record
    }

    record MultilegFlight(SimpleFlight flight1, SimpleFlight flight2) 
            implements Flight {
        // content from the existing MultilegFlight record
    }
}
```

#### Creating a CompanyServerResponse Sealed Interface

At some point, you need a clean model for the response you get from the Company server. Let us model this response with a sealed interface and records. For that, let us create a `CompanyServerResponse` sealed interface in a `response` package. You can then create the record you needs as member of this interface. In that case you do not need to add a `permit` clause to your sealed interface. 

1. `SimpleFlight`: built on a simple flight, with a price.
2. `MultilegFlight`: built on a multileg flight, with a price.
3. `NoFlight`: built on an error message, telling that the Json you got does not contain any flight. 
4. `Error`: built on an error message, telling that the server was unreachable, or that no response could be obtained. 

Your sealed interface should have this structure. 

```java
public sealed interface CompanyServerResponse {
    
    record SimpleFlight(Flight.SimpleFlight simpleFlight, int price)
            implements CompanyServerResponse {}
    
    record MultilegFlight(Flight.MultilegFlight multilegFlight, int price)
            implements CompanyServerResponse {}
    
    record NoFlight(String message)
            implements CompanyServerResponse {}
    
    record Error(String message)
            implements CompanyServerResponse {}
}
```

Let us move the technical code that consists in analyzing the Json you get to create your instances of `CompanyServerResponse.SimpleFlight` and `CompanyServerResponse.MultilegFlight`. 

Following the patterns used in the Flight.SimpleFlight and Flight.MultilegFlight records, you can also add utility constructors to the corresponding records in the `CompanyServerResponse` interface. 

The `CompanyServerResponse.SimpleFlight` record becomes the following. You can also add some validity checks to make sure this record does not carry any corrupted state. You can see that we are using this nice preview feature of the JDK 24: Flexible Constructor Bodies (JEP 492: https://openjdk.org/jeps/492), which consists in adding some code before a call to `this()` or `super()`.  

```java
record SimpleFlight(Flight.SimpleFlight simpleFlight, int price)
        implements CompanyServerResponse {

    public SimpleFlight {
        Objects.requireNonNull(simpleFlight);
    }
    
    // Utility constructor
    public SimpleFlight(String from, String to, int price) {
        var simpleFlight = new Flight.SimpleFlight(from, to);
        this(simpleFlight, price);
    }
    
    public static CompanyServerResponse.SimpleFlight of(JsonObject jsonObject) {
        var jsonFlight = jsonObject.getJsonObject("flight");
        var from = jsonFlight.getJsonObject("from").getString("name");
        var to = jsonFlight.getJsonObject("to").getString("name");
        var price = jsonObject.getInt("price");
        var simpleFlight = new CompanyServerResponse.SimpleFlight(from, to, price);
        return simpleFlight;
    }
}
```

And you can do the same for `CompanyServerResponse.MultilegFlight` record, that becomes the following.

```java
record MultilegFlight(Flight.MultilegFlight multilegFlight, int price)
        implements CompanyServerResponse {
    
    public MultilegFlight {
        Objects.requireNonNull(multilegFlight);
    }
    
    public MultilegFlight(String from, String via, String to, int price) {
        var multilegFlight = new Flight.MultilegFlight(from, via, to);
        this (multilegFlight, price);
    }
    
    public static CompanyServerResponse.MultilegFlight of(JsonObject jsonObject) {
        var jsonFlight = jsonObject.getJsonObject("multilegFlight");
        var from = jsonFlight.getJsonObject("from").getString("name");
        var to = jsonFlight.getJsonObject("to").getString("name");
        var via = jsonFlight.getJsonObject("via").getString("name");
        var price = jsonObject.getInt("price");
        var multilegFlight = new CompanyServerResponse.MultilegFlight(from, via, to, price);
        return multilegFlight;
    }
}
```

With these two methods, you can now build a similar factory method on the `CompanyServerResponse` sealed interface itself, that can deal with the `HttpResponse` directly, and return the right record. 

```java
static CompanyServerResponse of(HttpClientResponse response) {
    if (response.status() == Status.OK_200) {
        var reader = Json.createReader(response.entity().inputStream());
        var jsonObject = reader.readObject();
        if (jsonObject.containsKey("multilegFlight")) {
            return MultilegFlight.of(jsonObject);
        } else if (jsonObject.containsKey("flight")) {
            return CompanyServerResponse.SimpleFlight.of(jsonObject);
        } else {
            return new NoFlight("No Flight available");
        }
    } else {
        var message = response.as(String.class);
        return new Error(message);
    }
}
```

On the calling side, the code at the `// FIXME: Company for loop` is now the following. 

```java
// FIXME: Company for loop
for (var company : companies) {
    try (var response = WebClient.builder()
            .baseUri(COMPANY_SERVER_URI).build()
            .post("/company/" + company.tag())
            .submit(flight)) {
        // FIXME: Company Server Response Analysis
        var companyServerResponse = CompanyServerResponse.of(response);
        // we need to do something here!
    }
}
```

#### Extracting to a Callable

As you can see, you are now in a situation where you can extract the processing of the response from a company server in `Callable`. It is based on three parameters: `COMPANY_SERVER_URI`, the `company` object, and the `flight`, which is your request. 

So let us build this `Callable` in a factory method. Note that this `Callable` does not have its type parameter, because there is something to think about in it. 

```java
static Callable
companyQuery(ClientUri clientURI, Company company, Flight.SimpleFlight flight) {
    return () -> {
        try (var response = WebClient.builder()
                .baseUri(clientURI).build()
                .post("/company/" + company.tag())
                .submit(flight)) {

            var companyServerResponse = CompanyServerResponse.of(response);
            // we need to do something here!
        }
    }
}
```

This code is not complete yet, and you need to understand what it should return. On one side this code is querying the servers from the flight companies, and on the other side, it needs to provide information to your application. So it needs to adapt the model used by the servers to the model your application is using. Since you do not have the hand on the model used by the servers (the Json you get could change), it is probably not a good idea to use the same record model for both sides. If the Json sent by the company servers changes, then you would need to change the adaptation code, but not your application, that remains isolated from these changes. 

And by the way, you need to keep track of the company along with the flight and the price returned by these servers, so even for this very simple example, you have a difference between the two record models.

So let us create another sealed interface `CompanyResponse`, with the same 4 records in it, to model the response of the company servers, but this time following the needs of your application. As you can see, each record carries the company, because your application needs it. 


```java
public sealed interface CompanyResponse {
    
    record PricedSimpleFlight(
            Company company, Flight.SimpleFlight simpleFlight, int price)
            implements CompanyResponse {
        public PricedSimpleFlight {
            Objects.requireNonNull(company);
            Objects.requireNonNull(simpleFlight);
        }
    }

    record PricedMultilegFlight(
            Company company, Flight.MultilegFlight multilegFlight, int price)
            implements CompanyResponse {
        public PricedMultilegFlight {
            Objects.requireNonNull(company);
            Objects.requireNonNull(multilegFlight);
        }
    }

    record NoFlight(Company company, String message)
            implements CompanyResponse {
        public NoFlight {
            Objects.requireNonNull(company);
            Objects.requireNonNull(message);
        }
    }

    record Error(Company company, String message)
            implements CompanyResponse {
        public Error {
            Objects.requireNonNull(company);
            Objects.requireNonNull(message);
        }
    }
}
```

Now that you have the two models, you need to adapt the former to the latter, and you can fix the type of this `Callable`. The adaptation itself can easily be done with a switch expression on record types, using deconstruction. 

Note several things. 

1. You are switching on a sealed type, so you do not need any `default` branch.
2. If, for some reason, you need to add or remove records from your sealed hierarchy, you will get a compiler error, so it will be easy to fix it. 
3. If you change the model of one of your `CompanyServerResponse` records, following a change in the Json for instance, then this code will not compile anymore, so it will be easy to fix it. 

```java
static Callable<CompanyResponse>
companyQuery(ClientUri clientURI, Company company, Flight.SimpleFlight flight) {
    return () -> {
        try (var response = WebClient.builder()
                .baseUri(clientURI).build()
                .post("/company/" + company.tag())
                .submit(flight)) {

            var companyServerResponse = CompanyServerResponse.of(response);
            return switch (companyServerResponse) {
                case CompanyServerResponse.SimpleFlight(
                        Flight.SimpleFlight simpleFlight, int price) -> 
                        new CompanyResponse.PricedSimpleFlight(
                                company, simpleFlight, price);
                case CompanyServerResponse.MultilegFlight(
                        Flight.MultilegFlight multilegFlight, int price) -> 
                        new CompanyResponse.PricedMultilegFlight(
                                company, multilegFlight, price);
                case CompanyServerResponse.NoFlight(String message) -> 
                        new CompanyResponse.NoFlight(company, message);
                case CompanyServerResponse.Error(String message) -> 
                        new CompanyResponse.Error(company, message);
            };
        }
    };
}
```

At this point you may get compiler errors because this class still imports the `Flight` class, instead of your `Flight` interface. So let us fix that. 

Note that the `flight` parameter that this method is taking is the flight built for the request on the company servers. It is just a simple transport object that should not be part of your record model. You can just create it as a local record class, and refactor your `queriedFlightFrom()` method with it. Let us call it `QueryFlight` to avoid name collisions with your `Flight` sealed interface. You also need to refactor the signature of your `companyQuery()` method, that becomes `companyQuery(ClientUri, Company, QueryFlight)`. It is important to make this record public, as the Json marshaller will need it. 

```java
public record QueryFlight(City from, City to) {}

static QueryFlight queriedFlightFrom(ServerRequest request) {
    var travelRequest = request.content().as(TravelRequest.class);
    var cityFrom = Cities.byName(travelRequest.from());
    var destinationCity = Cities.byName(travelRequest.to());
    var flight = new QueryFlight(cityFrom, destinationCity);
    return flight;
}
```

Now this class should not be referencing the old `Flight` class anymore, so fixing your imports should fix your compiler errors.  

With all these elements the for-loop in your `travelHandler()` method becomes the following. You still need to refactor the `queriedFlightFrom()` method so that it can return the right `Flight` object, and you should name it `queryFlight`. 

```java
// FIXME: Company for loop
for (var company: companies) {
    var task = companyQuery(COMPANY_SERVER_URI, company, queryFlight);
    var companyResponse = task.call();
    // do something with the company response
}
```

#### Analyzing the result of the Callable

We have just one element to fix now (plus a very last one, bear with me!), which is the processing of this `companyResponse` return by this callable. The previous code was doing the following with this result.

1. If it is a flight or multileg flight, then add it to the `companyPricedTravels` collection, to get the cheapest price at the end. 
2. If there was an error, add the company to the `errorCompanies` collection, to deal with it later. 

So we have four records, that we need to distribute among two collections. A switch could do this job nicely, with the following code. 

```java
var companyPricedTravels = new ArrayList<CompanyResponse>();
var errorCompanies = new ArrayList<Company>();
// FIXME: Company for loop
for (var company: companies) {
    var task = companyQuery(COMPANY_SERVER_URI, company, flight);
    var companyResponse = task.call();
    switch (companyResponse) {
        case CompanyResponse.PricedSimpleFlight f -> companyPricedTravels.add(f);
        case CompanyResponse.PricedMultilegFlight f -> companyPricedTravels.add(f);
        case CompanyResponse.NoFlight _ -> errorCompanies.add(company);
        case CompanyResponse.Error _ -> errorCompanies.add(company);
    }
}
```

But you can do better, by creating more interfaces in your `CompanyResponse` sealed hierarchy. What would be nice to write would be the following, and while you are at it, update the type of the `companyPricedTravels` collection. 

```java
var companyPricedTravels = new ArrayList<CompanyResponse.Priced>();
var errorCompanies = new ArrayList<Company>();
// FIXME: Company for loop
for (var company: companies) {
    var task = companyQuery(COMPANY_SERVER_URI, company, flight);
    var companyResponse = task.call();
    switch (companyResponse) {
        case CompanyResponse.Priced priced -> companyPricedTravels.add(priced);
        case CompanyResponse.Failed _ -> errorCompanies.add(company);
    }
}
```

For that, you just need to create two sealed interfaces in the `CompanyResponse` interface: `Priced` and `Failed`, and have `PricedSimpleFlight` and `PricedMultilegFlight` to implement `Priced`, and `NoFlight` and `Error` implement `Failed`. 

```java
public sealed interface CompanyResponse {

    sealed interface Priced extends CompanyResponse {}

    sealed interface Failed extends CompanyResponse {}

    record PricedSimpleFlight(
            Company company, Flight.SimpleFlight simpleFlight, int price)
            implements Priced { ... }

    record PricedMultilegFlight(
            Company company, Flight.MultilegFlight multilegFlight, int price)
            implements Priced { ... }

    record NoFlight(Company company, String message)
            implements Failed { ... }

    record Error(Company company, String message)
            implements Fail { ... }
}
```

Written in that way, the `CompanyResponse` type has only two extensions: `Priced` and `Failed`. This is what you need if you want your switch in the `// FIXME: Company for loop` to be exhaustive. 

This approach is interesting, because it creates another layer of isolation between your record model and your business code. Here you are not interested in the real nature of your records, only by their behavior: do they have a price, or do they denote an error. 

#### Fixing the Best Flight

There are several pieces of code that are not compiling, one of which is the analysis of the flights to get the cheapest one. 

```java
// FIXME: best flight
var bestFlightOpt = companyPricedTravels.stream()
        .min(Comparator.comparingInt(CompanyPricedTravelDTO::price));
```

Fixing it is actually simple, since the type of the `companyPricedTravels` collection is now `List<CompanyResponse.Priced>`. You can add a `price()` method on the `CompanyResponse.Priced` interface you created, and refactor this code in this way. It does not follow the exact principles of Data Oriented Programming, as you are not supposed to add any behavior in your interfaces. But it is still very tempting. 

```java
// FIXME: best flight
var bestFlightOpt = companyPricedTravels.stream()
        .min(Comparator.comparingInt(CompanyResponse.Priced::price));
```

In case you do not want to add this `price()` method, because you want to stick strictly to the principles of Data Oriented Programming, then you need to switch on the concrete types of the `CompanyResponse` sealed interface and extract the price of the corresponding records. 


#### Fixing the Errors in the Weather Agency Responses Analysis

There are also two elements that are not compiling, due to the change of the type of the `bestFlight`: the construction of the two records `PricedTravelWithWeatherDTO` and `PricedTravelNoWeatherDTO`. All you need to do is to refactor the type of their first component to `CompanyResponse.Priced`. 

```java
record PricedTravelWithWeatherDTO(
        CompanyResponse.Priced travel, Weather weather) { ... }
```

```java
record PricedTravelNoWeatherDTO(
        CompanyResponse.Priced companyPricedTravel, WeatherErrorMessage error) { ... }
```

Don't worry, you will get rid of these DTOs soon enough. 

### Running the Application Again

At this point you should be able to run the application, but before that you need to update the JavaScript code that is analyzing the Json sent by you server. This Json is generated from the record you send, and they have been changed in the previous step. 

You need to locate the `C_Travel-agency-server/resources/static-content/travelpage.html` page, and change its content. The correct code to display the travels is now the following (you can look for the `// FIXME: Displaying travels` comment to find it in the page). 

```java
// FIXME: Displaying travels
if (response.travel.simpleFlight) {
    addTableData(response.travel.simpleFlight.from.name);
    addTableData("-");
    addTableData(response.travel.simpleFlight.to.name);
} else {
    addTableData(response.travel.multilegFlight.flight1.from.name);
    addTableData(response.travel.multilegFlight.flight1.to.name);
    addTableData(response.travel.multilegFlight.flight2.to.name);
}
```

[<< Return to top](#table-of-contents)

## Using the Structured Concurrency API to Query a Travel

This part starts on the branch `Step-01_SC-to-query-a-travel`. There is also a tag of the same name, to keep a pointer on the right commit in case you want to add some code in this branch.

### Submitting the Callables and Joining the StructuredTaskScope

Instead of launching each request one by one, and waiting for the result of the first one to launch the next one, the right approach is to launch all these requests in parallel. You could do that using the `ExecutorService` pattern, we are going to use the Structured Concurrency API. 

Let us add the following code, that wraps the for loop that you have. You need to open a `StructuredTaskScope` object with the following pattern. This scope wraps all your code, including the weather query. It starts at the `// FIXME: Company for loop` and wraps all the code until `// FIXME: Company scope end`. 

```java
// FIXME: Company for loop
try (var companyScope = StructuredTaskScope.<CompanyResponse>open()) {

}
// // FIXME: Company scope end
```

As you can see, you need to use the `open()` factory method to create such an instance, and to give a value to the type parameter this factory method needs. This type is the type of the `Callable` you are going to submit. In the case where you need to submit `Runnable` this type is simply `Void`. 

In this _try-with-resources_ block, you need to loop through the companies you have, create the callables, and submit them to this `StructuredTaskScope`. This code looks like the following. This code is not quite complete, we will come back to it in a minute. This `subtask` object is an object from the Structured Concurrency API. For now, you can think of it as being a future. 

```java
for (var company : Companies.companies()) {
    var task = companyQuery(COMPANY_SERVER_URI, company, flight);
    var subtask = companyScope.fork(task);
    // do something with subtask
}
```

Once you have submitted all your tasks, you need to call the `join()` of your `StructuredTaskScope` object. There are cases where this method returns something that you need to analyze, you will see that later. The `StructuredTaskScope` that you are using now makes it so that this `join()` call returns `Void`. 

```java
companyScope.join();
```

This `join()` call is a blocking call, that returns once all the tasks you submitted are done. They may have completed with a result, or with an exception.  

So now you need to analyze the `subtask` objects, meaning that you need to put them in a list to be able to use them here. Maybe it is the right time to convert this for loop to a stream. This code replaces the for loop over the companies. It is an improvement over the for loop, but it is not in its final state, so do not copy and paste it now. You will see its final version in a minute. 

```java
var companySubtasks = Companies.companies()
      .stream()
      .map(company -> companyQuery(COMPANY_SERVER_URI, company, flight))
      .map(scope::fork)
      .toList();
```

Now you have the list of the subtasks to work with, and you have joined your `StructuredTaskScope`, you can analyze your subtasks. 

### Analyzing the Joined Subtasks

Once the `scope.join()` method returns, you know that all your tasks are done. All you need to do it to analyze their contents, that can be a result, or an exception. 

Note that you cannot call `get()` on your subtasks before calling `scope.join()`. You will get an exception if you do that.

The application you are working on puts all the resuts in a list called `companyPricedTravels`, and in case of an exception, adds the corresponding company to a list `errorCompanies`. So we are missing an information in the list we have, that only contains the `subtask` objects. We need to keep track of the company corresponding to the `subtask`. There is a small refactoring to be made to have that. 

Let us create a local record to hold both the company and the subtask, that we can call `CompanyTask`. Note that you can create this record along with the stream you are creating, within the method. 

```java
record CompanyTask(Company company, Subtask<CompanyResponse> task) {}

var companySubtasks = Companies.companies()
      .stream()
      .map(company -> new CompanyTask(
            company,
              companyScope.fork(companyQuery(COMPANY_SERVER_URI, company, queryFlight))))
      .toList();

companyScope.join();
```

Once you created this stream, you can get rid of this variable: 

```java
var companies = Companies.companies();
```

The `companySubtasks` list is a list of `CompanyTask` objects that you can analyze to get the results in the one hand, and the company that could not be queried in the other hand. 

To know if a `subtask` contains a result or an exception, you can call its `state()` method, that returns a `Subtask.State` instance. This is an enumeration that defines three values: `SUCCESS`, `FAILED`, and `UNAVAILABLE` in case this task is still running. This should not happen if you check the state of a `subtask` after a call to `scope.join()`.

The `FAILED` state is there when your callable threw an exception. This is not the case in your application: your callable is created in the `companyQuery()` method, and does not throw any exception. 

Separating a collection of objects in two parts on a given criteria is something the Stream API can do thanks to a dedicated collector called the `partionningBy()` collector. The pattern is the following. 

```java
var map = companySubtasks.stream()
      .collect(
            Collectors.partitioningBy(
                  e -> e.task().state() == StructuredTaskScope.Subtask.State.SUCCESS &&
                       e.task().get() instanceof CompanyResponse.Priced
            )
      );
```

This map has two keys: `TRUE` and `FALSE`, each bound to a list with the corresponding `companySubtasks` in them. Now you need to get these lists and get the results, or the company that did not provide a price for the flight you are querying. Note that some of the successful tasks actually contain errors. This is the case for the `CompanyResponse.NoFlight` and `CompanyResponse.Error`, so we need to add them to the errors. 

### Getting the Price of the Flights

Getting the price of the flights is a simple stream pattern. You need to get the `companySubtasks` from the `CompanyTask` instances, and open them. You know that there is a result in them, since you checked that in the previous step. The mapping using `CompanyResponse.Priced.class::cast` is there to make your result a `List<CompanyResponse.Priced>` objects. This declaration replaces the previous declaration of `companyPricedTravels`. 

```java
var companyPricedTravels =
      map.get(true).stream()
            .map(CompanyTask::task)
            .map(StructuredTaskScope.Subtask::get)
            .map(CompanyResponse.Priced.class::cast)
            .toList();
```

### Getting the Faulty Companies

And the same goes for the companies that did not provide a price for the flight. Note that you can get the exact error message by calling `subtask.exception()` that gives you the exception that was thrown by your callable. This is not use here, because we decided to propagate errors using specific records, not exceptions. This declaration replaces the previous declaration of `errorCompanies`.

```java
var errorCompanies =
      map.get(false).stream()
            .map(CompanyTask::company)
            .toList();
```

### Running the Application Again

At this point you can run the application again. Now all your company servers are queried in parallel, which is an improvement. 

[<< Return to top](#table-of-contents)

## Updating the StructuredTaskScope Strategy to Manage Exceptions

This part starts on the branch `Step-02_Update-SC-strategy`. There is also a tag of the same name, to keep a pointer on the right commit in case you want to add some code in this branch.

### What Can Happen if an Exception is Thrown? 

So far the application you have catches the exception it may see and wraps them in specific types. What would happen if real exceptions were thrown?  

The default strategy implemented by the `StructuredTaskScope` default instance is the following. If one task fails, then the whole process fails, with the exception that was thrown by this task. You can simulate that in the callable returned by your `companyQuery()` method. Instead of returning a `CompanyResponse.NoFlight` record for instance, you could throw an unchecked exception, and see that it would break your application. 

```java
return switch (companyServerResponse) {
    case CompanyServerResponse.SimpleFlight(
            Flight.SimpleFlight simpleFlight, int price) ->
            new CompanyResponse.PricedSimpleFlight(company, simpleFlight, price);
    case CompanyServerResponse.MultilegFlight(
            Flight.MultilegFlight multilegFlight, int price) ->
            new CompanyResponse.PricedMultilegFlight(company, multilegFlight, price);
    case CompanyServerResponse.NoFlight(String message) ->
            // new CompanyResponse.NoFlight(company, message);
            throw new IllegalStateException("No flight");
    case CompanyServerResponse.Error(String message) ->
            // new CompanyResponse.Error(company, message);
            throw new IllegalStateException("Error");
};
```

This is not a great way of doing things, since a single faulty company server would prevent you from sending the other results to your client. 

Fortunately, the Structured Concurrency API gives you other strategies, and gives you the possibility to create your own. 

A strategy is defined by a `Joiner` object, that you need to pass to the `StructuredTaskScope.open()` method. This `Joiner` object is defined by two types: the type of your `Callable`, that is `Void` if you fork `Runnable` instead, and the type returned by the `scope.join()` method. 

You have currently 5 strategies to choose from. 

1. `Joiner.awaitAll()`. In that case the type returned by `join()` is `Void`. This joiner waits for all your tasks to complete, whether normally or exceptionally. This joiner does not cancel the scope in case of an exception. 
2. `Joiner.awaitAllSuccessfulOrThrow()`. In that case the type returned by `join()` is `Void`. This joiner waits for all your tasks to complete, whether normally or exceptionally. This joiner does cancel the scope in case of an exception.
3. `Joiner.allSuccessfulOrThrow()`. This joiner throws an exception as soon as a single task throws an exception. The `scope.join()` method return a stream of the results of your tasks.
4. `Joiner.anySuccessfulResultOrThrow()`. This joiner returns the first task to complete successfully. The `scope.join()` method immediately returns this result, and fails if no task completed successfully. The second type of this joiner is the same as the first one. 
5. `Joiner.allUntil(Predicate<SubTask)`. This `scope.join()` method returns a stream of the subtasks that are done, in their `fork()` order. Each of these task can carry a result, or an exception. The predicate you pass as an argument is used to test each of these tasks. If at some point it returns `true`, then the scope is cancelled, meaning that all the remaining tasks are interrupted and their thread is cleaned up. These tasks are still pushed to the stream returned by `scope.join()`, in the `UNAVAILABLE` state. Note that your predicate needs to be thread safe, as it will be invoked from different threads. 

### Choosing the Right Strategy

The strategy you need for this use case is the first one. A query that throws an exception should not cancel your scope. 

The declaration of your scope should now look like the following. 

```java
try (var companyScope =
           StructuredTaskScope.<CompanyResponse, Void>open(
                     StructuredTaskScope.Joiner.awaitAll()
           )) {
    
    // this code does not change
}
```

### Running the Application Again

You can now run your application again, and even if a company does not give you a response for a given flight, it should not break your application. 

Now that all your request are conducted in parallel, you can see that your application responds much faster than previously. 

[<< Return to top](#table-of-contents)

## Using the Structured Concurrency API to Query the Weather Forecast

This part starts on the branch `Step-03_SC-to-query-weather`. There is also a tag of the same name, to keep a pointer on the right commit in case you want to add some code in this branch.

### Querying the Weather Forecast Servers with a StructuredTaskScope

The following of the code queries your weather forecast servers. And here again, all the queries are made one after the other, which is even more stupid than for the flights, since only the first one is taken into account!

There is room for improvement also in this part of the code, and an excellent use case for your new friend the Structured Concurrency API. This time, the strategy you need is: give me the first result, and throw an exception if no server gave any. The exact strategy you need is the Strategy 4: `Joiner.anySuccessfulResultOrThrow()`. 

Following the same approach as in the previous section, you can make the for loop a stream, but this time you do not need the name of the weather agency along with the result, so you do not need to create a record to carry this binding. 

Note that using the `Joiner.anySuccessfulResultOrThrow()` strategy gives you a scope that returns your result on the `scope.join()` call. 

This `join()` call will now throw an exception if no weather server provide any response. You can catch it and implement your business logic in that case. 

#### Creating a Callable to Query the Weather Agency Servers

First, you can extract the code in the weather for loop `// FIXME: weather agencies for loop` to a method that returns a `Callable`, just as you did for the querying of the flight company servers. Note that this `Callable` throws an exception if no weather forecast is found, something that can be managed properly by the right `Joiner`, and that you can avoid with the same technique as the one used for the flight companies. 

```java
static Callable<Weather>
weatherQuery(ClientUri clientURI, WeatherAgency agency, City city) {
    return () -> {
        try (var response = WebClient.builder()
                .baseUri(clientURI).build()
                .post("/weather/" + agency.tag())
                .submit(city)) {
            if (response.status() == Status.OK_200) {
                var weather = response.as(Weather.class);
                return weather;
            } else {
                throw new IllegalStateException("No weather from " + agency.name());
            }
        }
    };
}
```

The weather for loop then becomes the following. 

```java
// FIXME: weather agencies for loop
for (var weatherAgency: WeatherAgencies.weatherAgencies()) {
    var task = weatherQuery(WEATHER_SERVER_URI, weatherAgency, destinationCity);
    try {
        var weather = task.call();
        weathers.add(weather);
    } catch (IllegalStateException e) {
        errorWeatherAgencies.add(weatherAgency);
    }
}
```

It is now easier to make it a stream and integrate it in a `StructuredTaskScope`, with the right strategy. Note that in this case the `weatherScope.join()` returns the first result returned by the tasks you submitted. Note that the two comments `// FIXME: weather agencies for loop` and `// FIXME: Weather scope end` give you the exact place where you need to put this code. 

```java
// FIXME: weather agencies for loop
try (var weatherScope = StructuredTaskScope.<Weather, Weather>open(
        StructuredTaskScope.Joiner.anySuccessfulResultOrThrow())) {
    
    WeatherAgencies.weatherAgencies().stream()
            .map(weatherAgency -> 
                weatherScope.fork(
                    weatherQuery(WEATHER_SERVER_URI, weatherAgency, destinationCity))
            )
            .toList();
    
    var weather = weatherScope.join();
    
    var pricedTravelWithWeather = new PricedTravelWithWeatherDTO(bestFlight, weather);
    res.status(Status.OK_200).send(pricedTravelWithWeather);
    
} catch (InterruptedException _) {
    var errorMessage = new WeatherErrorMessage("Weather not available");
    var pricedTravelNoWeather = new PricedTravelNoWeatherDTO(bestFlight, errorMessage);
    res.status(Status.OK_200).send(pricedTravelNoWeather);
}
// FIXME: Weather scope end
```

There is still a major code smell here. Instead of returning a result, or throwing an exception, you send the response directly from with the `weatherScope`. This is something that we will be taking care of in a minute.  

You can also get rid of the two collections `weathers` and `errorWeatherAgencies` that you do not need anymore. 

```java
var weathers = new ArrayList<Weather>();
var errorWeatherAgencies = new ArrayList<WeatherAgency>();
```

Note also that this scope will interrupt any running task when it gets the first result. Meaning that, on the Weather server side, you may have exceptions because of sockets that are abruptly closed while the server is sending some data. You can avoid these stack traces in your console by catching this `UncheckedIOException` exception (it is a runtime exception) in the method `WeatherAgency.handler()`. This exception may be thrown by the `response.send()` method call. And you can also handle this exception properly, which is not really the case in this example. 

```java
try {
    response.send(Weather.randomFor(name));
} catch (UncheckedIOException e) {
    System.out.println("Sending the weather was interrupted for " + name);
}
```

[<< Return to top](#table-of-contents)

## Using the Structured Concurrency API to Query a Travel in Parallel

This part starts on the branch `Step-04-SC-to-query-flight-and-weather`. There is also a tag of the same name, to keep a pointer on the right commit in case you want to add some code in this branch.

### Refactoring the Querying of the Companies

At this point your code base is still messy, even if it is probably not as messy as it was in the beginning. There is still something you need to fix: even if querying the flight prices and the weather forecast is now done in parallel, we still query the flight prices, and then the weather forecast, where it should be done in parallel. Fixing that will require some more refactoring in your record model. 

Let us move the code that query the weather outside the flight price scope. These are the lines following `// FIXME: weather agencies for loop`, that you can move outside of the block of the `travelScope`, that is after the closing of the flight query scope. Now the structure of your handler should be the following. 

```java
// FIXME: Company for loop
try (var companyScope = ... ) {
    ...
}

// FIXME: weather agencies for loop
try (var weatherScope = ...) {
    ...
}
```

Your code is not compiling anymore because the weather scope block needs this `bestFlight` variables, but for some wrong reasons. We will fix that in a minute.  

First, let us extract the block containing the travel scope to a method. What you need is to have a method that returns the best flight, or an error record in case no company can provide a response. So the structure of the code that calls this method is the following. 

```java
// FIXME: Company for loop
var bestFlight = queryCompanyServer(COMPANY_SERVER_URI, queryFlight);
```

And the structure of your method is the following. 

```java
static CompanyResponse 
queryCompanyServer(ClientUri COMPANY_SERVER_URI, QueryFlight queryFlight) 
        throws InterruptedException {

    try (var travelScope = ...) {
        
        // the forking of the tasks is the same

        travelScope.join();
        
        // the analysis of the result is the same
        
        var bestFlightOpt = companyPricedTravels.stream()
                .min(Comparator.comparingInt(CompanyResponse.Priced::price));
        if (bestFlightOpt.isPresent()) {
            var bestFlight = bestFlightOpt.orElseThrow();
            return bestFlight;
        } else {
            // what should be done?
        }
    } catch (InterruptedException e) {
        // what should be done?
    }
}
```

What should be done in the case where no best flight is found, or when an exception is caught? We are in a situation where no company could provide a response. It turns out that your `CompanyServer` hierarchy does not support that. So you need a new record in it, in the `Failed` hierarchy, that you could call `NoFlightFromAnyCompany`. 

```java
// in the CompanyResponse interface
record NoFlightFromAnyCompany(String message) 
implements Failed {
    public NoFlightFromAnyCompany {
        Objects.requireNonNull(message);
    }
}
```

Now, if no best flight is found, or if an exception is thrown, you can return this record. 

```java
        if (bestFlightOpt.isPresent()) {
            var bestFlight = bestFlightOpt.orElseThrow();
            return bestFlight;
        } else {
            // what should be done?
            return new CompanyResponse.NoFlightFromAnyCompany("No Flight found");
        }
    } catch (InterruptedException e) {
        // what should be done?
        return new CompanyResponse.NoFlightFromAnyCompany("Process interrupted: " + e.getMessage());
    }
}

```

### Refactoring the Querying of the Weather Agencies

At this point, you should still have some compiler errors in the code that queries the weather agencies. 

First, let us extract all this scope to a method. This method is built on the same model as the `queryCompanyServer()` method: it returns the weather forecast. 

Here is the code that is calling this method. 

```java
// FIXME: weather agencies for loop
var weather = queryWeatherAgencies(WEATHER_SERVER_URI, destinationCity);
```

Let us follow the same pattern to design the `queryWeatherAgencies()` method as the one we used for the flight companies. 

#### Creating a WeatherResponse Hierarchy

First, let us create a `WeatherResponse` sealed interface, with two records: `Weather` and `NoWeather`. You can store this interface in the `response` package. 

```java
public sealed interface WeatherResponse {

    record Weather(WeatherAgency agency, String weather)
            implements WeatherResponse {
        public Weather {
            Objects.requireNonNull(agency);
            Objects.requireNonNull(weather);
        }
    }

    record NoWeather(WeatherAgency agency)
            implements WeatherResponse {
        public NoWeather {
            Objects.requireNonNull(agency);
        }
    }
}
```

#### Refactoring the weatherQuery() Method

Now let us refactor the `weatherQuery()` method to use `WeatherResponse`. Instead of returning a `Callable<Weather>` and throwing an exception when something goes wrong, you can choose not to throw any exception, and return either a `Weather` or a `NoWeather`. This method can become the following. 

```java
static Callable<WeatherResponse>
weatherQuery(ClientUri clientURI, WeatherAgency agency, City city) {
    return () -> {
        try (var response = WebClient.builder()
                .baseUri(clientURI).build()
                .post("/weather/" + agency.tag())
                .submit(city)) {
            if (response.status() == Status.OK_200) {
                var weather = response.as(Weather.class);
                return new WeatherResponse.Weather(agency, weather.weather());
            } else {
                return new WeatherResponse.NoWeather(agency);
            }
        }
    };
}
```

You can now create the `queryWeatherServer()` method, following the example of the `queryCompanyServer()` method.  

```java
static WeatherResponse
queryWeatherServer(ClientUri WEATHER_SERVER_URI, City destinationCity) 
        throws InterruptedException {
    try (var weatherScope = StructuredTaskScope.<WeatherResponse, WeatherResponse>open(
            StructuredTaskScope.Joiner.anySuccessfulResultOrThrow())) {
        
        var weatherSubtasks = WeatherAgencies.weatherAgencies()
            .stream()
            .map(weatherAgency ->
                weatherScope.fork(
                    weatherQuery(WEATHER_SERVER_URI, weatherAgency, destinationCity)))
            .toList();
        var weatherResponse = weatherScope.join();
        return weatherResponse;
    }
}
```

The handler is also much simpler now. It is still spaghetti code, but with a single spaghetti. Maybe a spaguetto then? 

Note that we changed the variable names to `companyResponse` instead of `bestFlight`, and to `weatherResponse` instead of `weather. This to reflect the change in the nature of these two objects. 

```java
// FIXME: Start of the spaghetti code
routingBuilder.post("/travel", (req, res) -> {
    // FIXME: Extracting the requested flight
    var queryFlight = queriedFlightFrom(req);
    var destinationCity = queryFlight.to();
    // FIXME: Company for loop
    var companyResponse = queryCompanyServer(COMPANY_SERVER_URI, queryFlight);
    // FIXME: Company scope end
    // FIXME: weather agencies for loop
    var weatherResponse = queryWeatherServer(WEATHER_SERVER_URI, destinationCity);
    // FIXME: Weather scope end
});
// FIXME: Handler end
```

That been said, there are still two problems with this code. 

1. The flights and the weather are still queried one after the other. 
2. The `res` variable is not used anymore: this handler does not send any response. Which is definitely something we need to fix!

So you cannot run the application at this point, it is not working anymore. 

### Analyzing the Best Flight and the Weather

To run it again, you need to analyze the two results that you have in `bestFlight` and `weather`. 

To be able to run the application again, you need to push something to the `res` response object provided in the handler. And at this point, you have three cases to consider. 

1. You have a flight and weather forecast. 
2. You have a flight but no weather forecast. 
3. You don't have a flight, and you don't care about the weather forecast. 

All these cases can be checked with the types of the `companyResponse` and `weatherResponse` variables. Since there are two types of flights available, you can create a switch with the following structure. Thanks to the use of sealed types this switch is exhaustive, so you do not need any default branch. The `travelResponse` object is the object you need to build to send as a response. 

```java
var travelResponse = switch (companyResponse) {
    case CompanyResponse.PricedSimpleFlight simpleFlight -> {}
    case CompanyResponse.PricedMultilegFlight multilegFlight -> {}
    case CompanyResponse.Fail error -> {}
};
```

Then, for the branches with a flight, you need to check for the weather: is it there or not? So you need to add some code in your switch, that now looks like the following. 

```java
var travelResponse = switch (companyResponse) {
    case CompanyResponse.PricedSimpleFlight simpleFlight -> {
        switch (weatherResponse) {
            case WeatherResponse.Weather weather -> {}
            case WeatherResponse.NoWeather error -> {}
        }
    }
    case CompanyResponse.PricedMultilegFlight multilegFlight -> {
        switch (weatherResponse) {
            case WeatherResponse.Weather weather -> {}
            case WeatherResponse.NoWeather error -> {}
        }
    }
    case CompanyResponse.Fail error -> {}
};
```

You can see that you have five cases to manage. As usual, you can create a sealed interface implemented by records to model all this. Let us call it `TravelResponse` and store it in the `response` package. This sealed interface looks like the following. This code omits all the validation rules that you should put in each record.

```java
public sealed interface TravelResponse {
    
    record SimpleFlightWeather(
            Company company, 
            Flight.SimpleFlight simpleFlight, 
            int price, Weather weather)
    implements TravelResponse {}
    
    record MultilegFlightWeather(
            Company company, 
            Flight.MultilegFlight multilegFlight,
            int price, Weather weather)
    implements TravelResponse {}
    
    record SimpleFlightNoWeather(
            Company company, 
            Flight.SimpleFlight simpleFlight, 
            int price)
    implements TravelResponse {}
    
    record MultilegFlightNoWeather(
            Company company, 
            Flight.MultilegFlight multilegFlight, 
            int price)
    implements TravelResponse {}
    
    record NoFlight(String message)
    implements TravelResponse {}
}
```

The complete code is a little complex. It deconstructs all the records it gets, and adapts them to the records of the `TravelResponse` sealed hierarchy. It also adds a `message()` method to the `CompanyResponse.Failed` interface, for convenience. 

One last thing: let us extract all this code to a buildReponse() method, to keep the code of the handler simple. 

```java
var travelResponse = buildResponse(companyResponse, weatherResponse);
```

The final code is the following.

```java
static TravelResponse 
buildResponse(CompanyResponse companyResponse, WeatherResponse weatherResponse) {
    return switch (companyResponse) {
        case CompanyResponse.PricedSimpleFlight(
                Company company, Flight.SimpleFlight simpleFlight, int price) ->
            switch (weatherResponse) {
                case WeatherResponse.Weather(
                    WeatherAgency agency, String weather) ->
                    new TravelResponse.SimpleFlightWeather(
                        company, simpleFlight, price, 
                        new Weather(agency.name(), weather));
                case WeatherResponse.NoWeather _ ->
                        new TravelResponse.SimpleFlightNoWeather(
                                company, simpleFlight, price);
            };
        case CompanyResponse.PricedMultilegFlight(
                Company company, Flight.MultilegFlight multilegFlight, int price) ->
            switch (weatherResponse) {
                case WeatherResponse.Weather(WeatherAgency agency, String weather) ->
                    new TravelResponse.MultilegFlightWeather(
                        company, multilegFlight, price, 
                        new Weather(agency.name(), weather));
                case WeatherResponse.NoWeather _ ->
                    new TravelResponse.MultilegFlightNoWeather(
                        company, multilegFlight, price);
            };
        case CompanyResponse.Failed error -> new TravelResponse.NoFlight(error.message());
    };
}
```

You now have a `travelResponse` variable of type `TravelResponse`, that you can push to the response `res`.

```java
res.status(Status.OK_200).send(travelResponse);
```

We are oversimplifying the problem here by always returning a code `200`. You could send different codes, and deal with errors in a better way by checking the exact type of `travelResponse` you have. 

You still need to update the `C_Travel-agency-server/resources/static-content/travelpage.html` page to properly display these new records. The correct code you now need is the following. 

```java
// FIXME: Displaying travels
if (response.simpleFlight) {
    addTableData(response.simpleFlight.from.name);
    addTableData("-");
    addTableData(response.simpleFlight.to.name);
} else {
    addTableData(response.multilegFlight.flight1.from.name);
    addTableData(response.multilegFlight.flight1.to.name);
    addTableData(response.multilegFlight.flight2.to.name);
}
addTableData(response.price);
addTableData(response.company.name);
```

### Running the Application Again

Now you can run your application again. 

### Removing some Unused Code

You are in a better situation now, with cleanly separated record models for your requests and response. There is a bunch of ugly code that is not used anymore, that you can get rid of. 

All the ugly DTOs that sit in the `dto` package can happily be removed. Both packages `flight.priced` and `flight.travel` can also be removed. The exceptions in `weather.exception` and `company.exception` can also be removed, as you are not relying on the classical exception system anymore. Good riddance!

You are left with a much cleaner structure for your model. 

1. Records for `City`, `Company`, and `Weather`. 
2. A sealed type to model your flights, called `Flight`, as you have two types of flights: `SimpleFlight` and `MultilegFlight`. 
3. And you have sealed type to model each of your response: `CompanyServerResponse`, `CompanyResponse`, `WeatherResponse`, and `TravelResponse`. Choosing such a structure cleanly isolates the elements of each request and response from each server. Any server update (in the Json sent or received for instance) only impacts the corresponding sealed type.  

### Querying the Flight and Weather in parallel

#### The Easy, Inefficient Way

Querying your flights and the weather forecast in parallel is something you know how to do with the Structured Concurrency API. You need to create a scope, submit two callables to them, get the results and create the response. 

The structure of the code would look like the following. 

```java
try (var travelScope = StructuredTaskScope.open(...)) {

    var companyResponseSubTask =
        travelScope.fork(() -> queryCompanyServer(COMPANY_SERVER_URI, flight));
    var weatherResponseSubTask =
        travelScope.fork(() -> queryWeatherServer(WEATHER_SERVER_URI, destinationCity));
    
    travelScope.join();

    var companyResponse = companyResponseSubTask.get();
    var weatherResponse = weatherResponseSubTask.get();

    var travelResponse = buildResponse(companyResponse, weatherResponse);
    res.status(Status.OK_200).send(travelResponse);
}
```

You would still need some work before doing that because you need to create a common super type for your callables, as a `StructuredTaskScope` object is parameterized by a single type, and you have two: `CompanyResponse` and `WeatherResponse`. It is actually easy enough: all you need to do is to create the following sealed interface and adjust `CompanyResponse` and `WeatherResponse` to extend it. Your compiler will remind you that you need to have your two interfaces `CompanyResponse` and `WeatherResponse` to extend `CompanyWeatherResponse`. 

```java
public sealed interface CompanyWeatherResponse
    permits CompanyResponse, WeatherResponse {
}
```

#### The Right Way of Querying the Flight and Weather in parallel

But this way of writing things is not exactly what you want to do. What you need it to send your response to your client as soon as possible, that is, as soon as you have a price. When you have a price, you need to check if you have a weather forecast. If you do, then you send it, but if you do not, then you send the price without the weather forecast. 

The nice thing is: the system you wrote already supports the sending of a response with a price and no weather forecast, all you need to do is to find a way to implement this behavior. 

Fortunately, the Structured Concurrency API has a solution for you. There is a special `Joiner` that takes a `Predicate` as a parameter. This predicate is invoked every time a task is done, either with a result or an exception. Then, if this predicate returns true, this `StructuredTaskScope` is immediately cancelled. All the tasks that are still running are interrupted and the `travelScope.join()` returns. This is exactly what you need: when you see the result for the flight, you cancel the scope and check if you have a result for the weather forecast.  

It turns out that in your system, the callables never throw exceptions, this is not how you deal with errors. Even if a server is not responding, you still return a record that tell you that. 

So the predicate you can write is the following. When you see the result for the flight you return true, and false if it is the result for the weather forecast. 

Since you never throw exceptions in this system, you should never see the `FAILED` state. 

The type of this predicate may look intimidating, it is imposed by the API. 

```java
// FIXME: Company for loop
Predicate<StructuredTaskScope.Subtask<? extends CompanyWeatherResponse>> 
        subtaskPredicate =
        
    subtask -> switch(subtask.state()) {
        case SUCCESS -> switch (subtask.get()) {
            case CompanyResponse _ -> true; // cancels the scope
            case WeatherResponse _ -> false; 
        };
        case FAILED -> 
            throw new IllegalStateException("Got a subtask in FAILED state");
        case UNAVAILABLE -> 
            throw new IllegalStateException("Got a subtask in UNAVAILABLE state");
    };
```

Analyzing the result you get is a little different. It does not change of the `companyResponseSubTask`, but you need to change the way you analyze the `weatherResponseSubTask`. Because the `travelScope` may have been cancelled before the weather forecast was received, it is possible that the `weatherResponseSubTask` is still in the `UNAVAILABLE` state. So your need to take this into account in your code. 

The final code becomes the following. 

The `Joiner` that you need is the `Joiner.allUntil(Predicate)`, that takes a `Predicate` as a parameter. This is the predicate you just wrote. 

You submit your tasks and join your scope as usual, this does not change. Note that the `travelScope.join()` returns a stream of your tasks. They can be in any state, as this stream contains all the remaining tasks that were cancelled when the predicate returned true. These are in the `UNAVAILABLE` state. We do not use this feature here, as we only have two tasks. 

Then you analyze the result. The code does not change for `companyResponseSubTask`, but it does for `weatherResponseSubTask` as it can be in an `UNAVAILABLE` state. Note that we created a new record to model this new behavior: `WeatherResponse.WeatherTimeout`. 

```java
try (var travelScope = StructuredTaskScope.open(
        StructuredTaskScope.Joiner.allUntil(subtaskPredicate)
)) {
    
    // First submit your tasks as usual
    var companyResponseSubTask =
        travelScope.fork(() -> queryCompanyServer(COMPANY_SERVER_URI, queryFlight));
    var weatherResponseSubTask =
        travelScope.fork(() -> queryWeatherServer(WEATHER_SERVER_URI, destinationCity));
    
    // then call join
    travelScope.join();
    
    // then analyze the results
    var companyResponse = companyResponseSubTask.get();
    var weatherResponse = switch(weatherResponseSubTask.state()) {
        case SUCCESS -> 
            weatherResponseSubTask.get();
        case UNAVAILABLE -> 
            new WeatherResponse.WeatherTimeout("Weather forecast took too long");
        case FAILED -> 
            throw new IllegalStateException("Got a weather subtask in FAILED state");
    };
    var travelResponse = buildResponse(companyResponse, weatherResponse);
    res.status(Status.OK_200).send(travelResponse);
}
```

Since you add a record in the `WeatherResponse` sealed hierarchy, a compiler error was generated in all the switch on this type that you have in your application. This is the case in the `buildResponse()` method that returns a `TravelResponse`. 

You can actually just update two cases in this switch. 

The following one. 

```java
case WeatherResponse.NoWeather _ ->
        new TravelResponse.SimpleFlightNoWeather(
                company, simpleFlight, price);
```

That can become this one. Note that you can use several unnamed patterns in the same case, precisely because they are unnamed. 

```java
case WeatherResponse.NoWeather _, WeatherResponse.WeatherTimeout _ ->
        new TravelResponse.SimpleFlightNoWeather(
                company, simpleFlight, price);
```

And the second one is this one. 

```java
case WeatherResponse.NoWeather _ ->
        new TravelResponse.MultilegFlightNoWeather(
        company, multilegFlight, price);
```

That can become this one, following the same pattern. 

```java
case WeatherResponse.NoWeather _, WeatherResponse.WeatherTimeout _ ->
        new TravelResponse.MultilegFlightNoWeather(
                company, multilegFlight, price);
```

#### Running the Application Again

At this point you can run the application again. Odds are that you will still see the weather forecast most of the time. If you really want to see this last feature in action, you can drastically increase the response time of the Weather Agencies server from the `files/weather-agencies.txt` file. Here we have multiplied it by 10. There is no way you can see any more weather forecast. 

```text
# Weather agencies
# Name, tag, average response time in ms, dispersion
Global Weather, global, 1000, 20
Star Weather, star, 800, 10
Planet Weather, planet, 900, 25
```

#### One Last Fix

When your travel server cancels a weather forecast task, it also closes the connection to your Weather Agencies server. It generates a Socket Exception on this server, that you may want to catch. You can do that in the `WeatherAgency.handle()` method in the `A_Weather-server` module. Catching this `UncheckedIOException` will do the trick. Note that you can also implement a smarter way to deal with this exception. 

```java
public Handler handler() {
    return (request, response) -> {
        var cityDTO = request.content().as(City.CityDTO.class);
        WeatherServer.sleepFor(average, dispersion);
        try {
            response.send(Weather.randomFor(name));
        } catch (UncheckedIOException e) {
            System.out.println(e.getMessage());
        }
    };
}
```

Congratulations, you have reached the end of this lab!

You can check the final version of this application on the branch `Step-05-Final-version`. There is also a tag of the same name, to keep a pointer on the right commit in case you want to add some code in this branch.

[<< Return to top](#table-of-contents)