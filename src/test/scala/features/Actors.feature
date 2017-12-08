Feature: Actors

  Scenario: Telling to an actor
    Given I instantiate an actor
    When I tell it something
    Then the corresponding method is executed

  Scenario: Asking a future from an actor
    Given I instantiate an actor
    When I ask it something that returns a Future
    Then the returned future will contain the answer

  Scenario: Asking an Actor from an actor
    Given I instantiate an actor
    When I ask it something that returns an Actor
    Then the returned actor will contain the answer

  Scenario: Asking something else from an actor
    Given I instantiate an actor
    When I ask it something that returns anything else
    Then the returned future will contain the answer

  Scenario: Telling a failed actor
    Given I instantiate an actor
    And I make it fail
    When I tell it something
    Then nothing happens

  Scenario: Asking a Future from a failed Actor
    Given I instantiate an actor
    And I make it fail
    When I ask it something that returns a Future
    Then it returns a failed Future

  Scenario: Asking an Actor from a failed Actor
    Given I instantiate an actor
    And I make it fail
    When I ask it again something that returns an Actor
    Then it returns another failed Actor

  Scenario: Asking something else from a failed Actor
    When I instantiate an actor
    And I make it fail
    When I ask it something that returns anything else
    Then it returns a failed Future
