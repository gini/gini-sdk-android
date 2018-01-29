.. _guide-getting-started:

===============
Getting started
===============


First of all: Add the Library to your Build
===========================================

The gini sdk is provided as a aar archive. You can integrate it in your gradle based project by adding it as
dependency. In order to gain access to the aar file, you have to add the Gini Maven repository to your build script.

.. code-block:: groovy

    repositories {
        maven {
            url "https://repo.gini.net/nexus/content/repositories/public"
        }
        ...
    }

    dependencies {
        compile ('net.gini:gini-android-sdk:1.4.2@aar'){
            transitive = true
        }
        ...
    }

Integrating the Gini SDK
========================


The Gini SDK provides the ``Gini`` class which is a façade to all functionality of the Gini SDK. We recommend using an
instance of this class singleton-like. By saying singleton-like we mean that you somehow manage to create and keep
one instance at application start. Instead of creating a new instance every time when you need to interact with the
Gini API, you reuse this instance. This has the benefits that the SDK can reuse sessions between requests to the
Gini API which may save a noteworthy number of HTTP requests.

Creating the Gini SDK instance
------------------------------

In order to create an instance of the ``Gini`` class, you need both your client id and your client secret. If you don't
have a client id and client secret yet, you need to register your application with Gini. `See the Gini API documentation
to find out how to register your Application with Gini <http://developer.gini.net/gini-api/html/guides/oauth2.html#first-of-all-register-your-application-with-gini>`_.

All requests to the Gini API are made on behalf of a user. This means particularly that all created documents are bound
to a specific user account. But since you are most likely only interested in the results of the semantic document
analysis and not in a cloud document storage system, the Gini API has the feature of "anonymous users". This means that
user accounts are created on the fly and the user account is unknown to your application's user.

The following example describes how to use the Gini API in your application with such anonymous user accounts. To use
the Gini API, you must create an instance of the ``Gini`` class. The ``Gini`` instance is configured and created with the
help of the ``SdkBuilder`` class. In this example, the anonymous users are created with the email domain "example.com".
An example of a username created with this configuration would be ``550e8400-e29b-11d4-a716-446655440000@example.com``

.. code-block:: java

    import net.gini.android.SdkBuilder;
    import net.gini.android.DocumentTaskManager;
    
    ...
    
    // The Gini instance is a facade to all available managers of the Gini SDK. Configure and create the SDK with
    // the SdkBuilder.
    Gini gini = new SdkBuilder(getContext(), "gini-client-id", "GiniClientSecret", "example.com").build();
    // The DocumentTaskManager provides the high-level API to work with documents.
    DocumentTaskManager documentManager = gini.getDocumentTaskManager();


Congratulations, you successfully integrated the Gini SDK. 
