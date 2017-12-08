Feature: Actors

  Scenario: Telling to an actor
    Given I instantiate an actor
    When I tell it something
    Then the corresponding method is executed

  Scenario: Asking a future from an actor
    Given I instantiate an actor
    When I ask it something that returns a Future
    Then the returned future will contain the answer

  Scenario: Asking a future from an actor
    Given I instantiate an actor
    When I ask it something that returns an Actor
    Then the returned actor will contain the answer

  Scenario: Asking a future from an actor
    Given I instantiate an actor
    When I ask it something that returns anything else
    Then the returned future will contain the answer
