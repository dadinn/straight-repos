# Indium
[![MELPA](https://melpa.org/packages/indium-badge.svg)](https://melpa.org/#/indium)
[![Emacs](https://img.shields.io/badge/Emacs-25-8e44bd.svg)](https://www.gnu.org/software/emacs/)
[![Build Status](https://drone.petton.fr/api/badges/nico/Indium/status.svg)](https://drone.petton.fr/nico/Indium)
[![Documentation Status](https://readthedocs.org/projects/indium/badge/?version=latest)](https://indium.readthedocs.io/en/latest/?badge=latest)
[![Liberapay](https://img.shields.io/liberapay/receives/NicolasPetton.svg?logo=liberapay)](https://img.shields.io/liberapay/receives/NicolasPetton.svg?logo=liberapay)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/indium-emacs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
<img align="right" src="https://gitea.petton.fr/nico/Indium/raw/branch/master/img/indium.png" alt="logo">

A JavaScript development environment for Emacs.

Indium connects to a browser tab or nodejs process and provides many features for
JavaScript development, including:

- a REPL (with auto completion) & object inspection;
- an inspector, with history and navigation;
- a scratch buffer (`M-x indium-scratch`);
- JavaScript evaluation in JS buffers with `indium-interaction-mode`;
- a stepping Debugger, similar to `edebug`, or `cider`.

## Documentation

Installation instruction and other documentation can be found on
[readthedocs](https://indium.readthedocs.io).

**WARNING**: Indium 2.0 now uses a client/server architecture, see [installation
instuctions](https://indium.readthedocs.io/en/latest/installation.html).

## Screenshots

The stepping debugger

![Debugger](./screenshots/debugger.gif)

The REPL with company-mode

![REPL](./screenshots/repl.png)

The REPL showing clickable output

![REPL](./screenshots/repl2.png)

The inspector

![Inspector](./screenshots/inspector.png)

## Missing features

Indium is young, here's a list of missing/wanted features

- ~~Code evaluation using `C-x C-e` from project JS buffers~~
- ~~Adding breakpoints (to remove the need for `debugger` statements)~~
- Network inspector (could get inspiration from restclient.el)
- DOM inspector
- ~~Start processes (chromium, node, grunt, gulp, etc.) from Emacs and attach a
  indium connection to it.~~

## Backends

There is currently support for Chrom[e|ium] & nodejs.  The firefox backend is
in the TODO list.


