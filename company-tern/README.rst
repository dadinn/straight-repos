Company tern
============

.. image:: https://travis-ci.org/proofit404/company-tern.png
    :target: https://travis-ci.org/proofit404/company-tern
    :alt: Build Status

Tern_ backend for company-mode_.

Installation
------------

You can install this package from Melpa_::

    M-x package-install RET company-tern RET

Usage
-----

Add ``company-tern`` to allowed ``company-mode`` backends list

.. code:: lisp

    (add-to-list 'company-backends 'company-tern)

If you don't like circles after object's own properties consider less
annoying marker for that purpose.

.. code:: lisp

    (setq company-tern-property-marker "")

You can trim too long function signatures to the frame width.

.. code:: lisp

    (setq company-tern-meta-as-single-line t)

If you doesn't like inline argument annotations appear with
corresponding identifiers, then you can to set up the company align
option.

.. code:: lisp

    (setq company-tooltip-align-annotations t)

Thanks
------

* **@katspaugh**
* **@dgutov**

.. _Tern: http://ternjs.net/
.. _company-mode: http://company-mode.github.io/
.. _Melpa: http://melpa.milkbox.net/
