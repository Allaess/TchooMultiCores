Feature: Actors

  Scenario: Starting an Application
    Given I instantiated a Root
    When I tell Root to start an Application
    Then the Application is instantiated
    And the Application is told to start

  Scenario: Application auto-restart on failure
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application fails
    Then Root restarts the Application

  Scenario: Telling an Actor
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application tells something to an Actor
    Then the Actor executes the corresponding method

  Scenario: Asking an Actor from another Actor
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application asks another Actor from an Actor
    Then the Actor returns the other Actor

  Scenario: Asking an Acting from an Actor
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application asks an Acting from an Actor
    Then the Actor returns the corresponding Actor

  Scenario: Asking a Future from an Actor
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application asks a Future from an Actor
    Then the Actor returns the Future

  Scenario: Asking a String from an Actor
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application asks a String from an Actor
    Then the Actor returns a Future

  Scenario: Mapping over an Actor
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application maps over an Actor
    Then the mapped Actor is returned

  Scenario: FlatMapping over an Actor
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application flatMaps over an Actor
    Then the flatMapped Actor is returned

  Scenario: Filtering over an Actor with success
    Given I instantiated a Root
    And I told Root to start an Application
    When the Application filters over an Actor with success
    Then the filtered Actor is returned
