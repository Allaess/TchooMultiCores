Feature: Actors

  Scenario: Telling an Actor
    Given I instantiate an Actor
    When I tell it something
    Then it executes the corresponding method
    And the receiving Actor is not failed

  Scenario: Asking an Actor from an Actor
    Given I instantiate an Actor
    When I ask it something that returns an Actor
    Then I get an Actor with the same behavior
    And the receiving Actor is not failed

  Scenario: Asking an Acting from an Actor
    Given I instantiate an Actor
    When I ask it something that returns an Acting
    Then I get an Actor with the same behavior
    And the receiving Actor is not failed

  Scenario: Asking a Future from an Actor
    Given I instantiate an Actor
    When I ask it something that returns a Future
    Then I get a Future that will complete to the same value
    And the receiving Actor is not failed

  Scenario: Asking something else from an Actor
    Given I instantiate an Actor
    When I ask it something that returns anything else
    Then I get a Future that will complete to the same value
    And the receiving Actor is not failed

  Scenario: Asking an Actor from an Actor that throws
    Given I instantiate an Actor
    When I ask it something that should return an Actor but throws
    Then I get a failed Actor with the same failure
    And the receiving Actor is not failed

  Scenario: Asking a failed Actor from an Actor
    Given I instantiate an Actor
    When I ask it something that returns a failed Actor
    Then I get a failed Actor with the same failure
    And the receiving Actor is failed

  Scenario: Asking an Acting from an Actor that throws
    Given I instantiate an Actor
    When I ask it something that should return an Acting but throws
    Then I get a failed Actor with the same failure
    And the receiving Actor is not failed

  Scenario: Asking a Future from an Actor that throws
    Given I instantiate an Actor
    When I ask it something that should return a Future but throws
    Then I get a failed Future with the same failure
    And the receiving Actor is not failed

  Scenario: Asking a failed Future from an Actor
    Given I instantiate an Actor
    When I ask it something that returns a failed Future
    Then I get a failed Future with the same failure
    And the receiving Actor is not failed

  Scenario: Asking something else from an Actor that throws
    Given I instantiate an Actor
    When I ask it something that should return something else but throws
    Then I get a failed Future with the same failure
    And the receiving Actor is not failed

  Scenario: Mapping an Actor
    Given I instantiate an Actor
    And I let it instantiate a second one
    When I let the first Actor map the second one
    Then I get a mapped Actor

  Scenario: FlatMapping an Actor
    Given I instantiate an Actor
    And I let it instantiate a second one
    And I let it instantiate a third one
    When I let the first Actor flatMap over the two others
    Then I get a flatMapped Actor

  Scenario: Filtering an Actor (matching)
    Given I instantiate an Actor
    And I let it instantiate a second one
    When I let the first Actor filter over the second one with a matching filter
    Then I get the Actor

  Scenario: Filtering an Actor (non-matching)
    Given I instantiate an Actor
    And I let it instantiate a second one
    When I let the first Actor filter over the second one with a non-matching filter
    Then I get an empty Actor
