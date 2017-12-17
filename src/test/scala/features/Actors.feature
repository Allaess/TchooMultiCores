Feature: Actors

  Scenario: Initializing Actor system
    Given I create a new Root Actor
    When I let it start a new App
    Then the new App instance receives the start() message

  Scenario: Telling an actor
    Given I create a new Root Actor
    And I let it start a new App
    When I tell it something
    Then it executes the told method

  Scenario: Asking an Actor from an Actor
    Given I create a new Root Actor
    And I let it start a new App
    When I ask it something that returns a Greeter Actor
    Then I receive a Greeter Actor

  Scenario: Asking an Acting from an Actor
    Given I create a new Root Actor
    And I let it start a new App
    When I ask it something that returns a Greeter Acting
    Then I receive a Greeter Actor

  Scenario: Asking a Future from an Actor
    Given I create a new Root Actor
    And I let it start a new App
    When I ask it something that returns a Future
    Then I receive a Future

  Scenario: Asking anything else from an Actor
    Given I create a new Root Actor
    And I let it start a new App
    When I ask it something that returns anything else
    Then I receive a Future
