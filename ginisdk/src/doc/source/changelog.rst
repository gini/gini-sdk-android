=========
Changelog
=========

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
