Gini Android SDK
================

An SDK for integrating Gini technology into other apps. With this SDK you will be able to extract semantic information
from various types of documents.


Dependencies
------------

The Gini Android SDK has only two dependencies:

* [Volley from Google](http://developer.android.com/training/volley/index.html) ([AOSP Repository](https://android.googlesource.com/platform/frameworks/volley))
* [Bolts from facebook](https://github.com/BoltsFramework/Bolts-Android)


Integration
-----------

The Gini Android SDK provides a builder which should be used to configure and create the Gini SDK. The builder's `build`
method returns a `Gini` instance, which gives access to all needed classes to work with the Gini API:

```java
import net.gini.android.SdkBuilder;
import net.gini.android.DocumentTaskManager;

...

Gini gini = new SdkBuilder(getContext(), "gini-client-id", "GiniClientSecret", "@example.com").build();
// The DocumentTaskManager provides the high-level API to work with documents.
DocumentTaskManager documentManager = gini.getDocumentTaskManager();

```

See the [Gini Android SDK documentation](http://developer.gini.net/gini-sdk-android/index.html?net/gini/android/DocumentTaskManager.html)
for more details how to use the `DocumentTaskManager`.


Copyright (c) 2014, [Gini GmbH](https://www.gini.net/)
