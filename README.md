[MineResetLite](https://polymart.org/resource/mineresetlite.137)
=============

Lightweight implementation for automatic mine resets.

You can get the latest build from [here](https://polymart.org/resource/mineresetlite.137) or [release](https://github.com/TeamVK/MineResetLite/releases).  The latest build includes new methods from MineResetLitePlus.

## Features:

- It supports non-cuboid shape mines such as Polygon2D, Cylinder, Ellipsoid in addition to a traditional cuboid.  You can use WorldEdit to specify such complex shape mines.
[![Video: Polygon shape Mine]](https://youtu.be/71dxuIUpRb4?si=g2oJilb-ZwlAAxyL)

- It works with v6, v7 and new-v7 WorldEdit (including FastAsyncWorldEdit).
- This version also supports 1.13 style block specification, it supports all the following formats:
  - STONE
  - 1
  - STONE:2
  - GRANITE
  - 1:2
  - <custom_block_name_ID> (if you have custom blocks from a plugin like ItemsAdder, you can put the custom block's ID).
- As of v3.0.0, features previously provided in MineResetLitePlus have been integrated (see "New commands & Permissions") 
- If you want to display various placeholders in Sign/Holograms, you should. use PlaceholderAPI.  This MineResetLite comes with built-in PlaceholderAPI expansion module (no need to install separate PAPI expansion).  You can use those placeholders HolographicExtension, PAPISign (example of creating MineResetLite sign is on PAPISign's overview page) and DynamicSigns.

