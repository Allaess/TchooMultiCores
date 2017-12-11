Feature: Actors

  Scenario: Telling to an Actor from non Actor code
    Given I instantiate an Actor
    When I tell it something
    Then it executes the told method

  Scenario: Telling an Actor from another Actor
    Given I instantiate an Actor
    And I let the first Actor instantiate another one
    When the first Actor tells something to the second one
    Then the second Actor executes the told function

  Scenario: Asking for a Future
    Given I instantiate an Actor
    When I ask it something that returns a Future
    Then I receive a future that will contain the answer

  Scenario: Asking for an Actor
    Given I instantiate an Actor
    And I let the first Actor instantiate another one
    When I tell the first Actor to ask the second Actor for something that returns a third Actor
    Then the first Actor receives an Actor that will contain the answer

  Scenario:Asking for something else
    Given I instantiate an Actor
    When I ask it something that returns neither an Actor neither a Future
    Then I receive a future that will contain the answer

  Scenario: Asking for a Future and getting a failed Future
    Given I instantiate an Actor
    When I ask it something that returns a failed Future
    Then I receive a Future that will fail with the same Exception

  Scenario: Asking for a Future and throwing
    Given I instantiate an Actor
    When I ask it something that should return a Future but throws
    Then I receive a Future that will fail with the same Exception

  Scenario: Asking for an Actor and throwing
    Given I instantiate an Actor
    When I ask it something that should return an Actor but throws
    Then I received a failed Actor
    And The Actor is failed with the same Exception

  Scenario: Asking for something else and throwing
    Given I instantiate an Actor
    When I ask it something that should return neither a Future neither an Actor but throws
    Then I receive a Future that will fail with the same Exception
