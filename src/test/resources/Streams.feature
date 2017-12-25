Feature: Reactive Streams

  Scenario: Publishing data
    Given I started an Application
    And a Subscriber subscribed to a SourcePublisher
    When the SourcePublisher publishes some data
    Then the Subscriber receives the data

  Scenario: Processing Data
    Given I started an Application
    And a Processor subscribed to a SourcePublisher
    And a Subscriber subscribed to the Processor
    When the SourcePublisher publishes some data
    Then the Subscriber receives the processed data
