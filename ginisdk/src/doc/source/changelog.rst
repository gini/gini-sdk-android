=========
Changelog
=========

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
