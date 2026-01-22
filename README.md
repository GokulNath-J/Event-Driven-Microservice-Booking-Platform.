# Project Name
Event Driven-Microservice-Booking Platform.

## Description

This is an event-driven microservices-based booking platform that demonstrates the core workflow of a train reservation system. The platform supports key functionalities such as adding trains, user registration, ticket booking, and ticket cancellation.

This project was built as part of my backend developer learning journey, where I implemented real-world concepts using Java, Spring Boot, REST APIs, and microservices architecture to understand how scalable backend systems work in practice.

## Tech Stack

Core Java 

Spring Boot

Spring Cloud (Eureka, Config Server, API Gateway)

Apache Kafka

MySQL

Docker

Kubernetes

Git & GitHub

## System Architecture


## API Endpoints

- Register New User
POST /user/register

- Login User
POST /user/login

- Create New E-Wallet
POST /user/ewallet

- Add Money to E-Wallet
POST /user/ewallet/add-money

- Book Ticket
POST /user/book

- Cancel Ticket
Put /user/bookingCancelRequest

- Get Train by train Number for normal booking 
Post /user/getTrainForNormalBookingByTrainNumber

- Get Train by train Number for tatkal booking 
Post /user/getTrainForTatkalBookingByTrainNumber

- Get Train by train Number for Premium tatkal booking
Post /user/getTrainForPremiunTatkalBookingByTrainNumber

- Add One Train 
Post /train/addonetrain

- Get All the Trains 
Get /train/getallTrains

- Get Premium and Tatkal Tickets Manually 
Get /booking/getPremiumTatkalTickets

## Database Design


## Project Structure

## Future Improvements




