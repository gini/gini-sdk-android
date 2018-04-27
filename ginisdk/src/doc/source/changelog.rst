=========
Changelog
=========

2.0.0-beta.3 (2018-04-27)
=========================

- Document polling can be cancelled.
- Updated json formats to match final versions.

2.0.0-beta.2 (2018-04-09)
=========================

- Delete method for partial documents which deletes its parents, too.
- Delete method for any document which fails for partial documents that have parents.

2.0.0-beta.1 (2018-04-04)
=========================

- Support for composite (multi-page) document analysis. A composite document consists of one or more partial documents.
For each page you need to create a partial document. Using these you can create a composite document for which you can
get the extractions after polling has finished.

1.5.0 (2018-02-08)
===================

- Min API Level increased to 15.
- Public key pinning with `Android Network Security Configuration and TrustKit <guides/getting-started.html#public-key-pinning>`_. Previous public key pinning configuration was removed.

1.4.3 (2018-02-05)
===================

Feature
-------

- Public key pinning.

1.4.2 (2018-01-29)
===================

Bugfix
------

- Using a custom cache doesn't prevent requests from being sent anymore.

1.4.1 (2018-01-15)
===================

Bugfixes
--------

- Added missing source classification types.
- Unknown source classification or unknown processing states don't cause exceptions anymore.

1.4.0 (2017-12-20)
===================

Feature
-------

- Certificate pinning.

1.3.92 (2017-04-13)
===================

Bugfixes
--------

- A new anonymous user will be generated if the existing one is invalid.
- Changing the email domain will be applied to existing anonymous users.

1.2.84 (2017-03-31)
===================

Feature
-------

- TLS is preferred over SSLv3 on API Levels 16 and later.

1.2.81 (2017-03-01)
===================

Feature
-------

- Volley caching can be customized by setting a com.android.volley.Cache implementation in the
  SdkBuilder.

1.1.73 (2016-12-21)
===================

Maintenance
-----------

- Updated Volley to com.android.Volley 1.0.0.

1.1.66 (2016-11-21)
===================

Bugfix
------

- Extraction candidates are now deserialized and returned as expected.

1.1.47 (2015-11-13)
===================

Feature
-------

- Documents can be uploaded as a byte array, too. Useful for texts (UTF-8 encoded) or PDFs.
- Document Type enum added and should be used instead of strings.
