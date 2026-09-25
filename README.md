# MlogAssertions

> [!TIP]
> A separate release is made for Mindustry Build 160 and Mindustry Build 154.2 or later (up to 159.7). Use the mod browser to install the correct version of the mod for your version of Mindustry.

> [!NOTE]
> Using this mod on maps with lots of processors may have a negative performance impact on the game.

This mod aims to make debugging in Mindustry Logic a bit easier. Provided functionality:

* Enhanced **Vars** screen: showing numeric values in full precision and color values, sorting variables by name, copying variables and the text buffer contents to the clipboard.
* **Memory** screen for inspecting contents of memory blocks, using the same interface as the **Vars** screen. Includes a command to clear (reset) the contents of memory blocks.
* Settings for overriding instruction limit (the limit can be increased up to 2000 instructions). This allows you to insert debugging code (e.g., additional `print` instructions) into your code, even if the original instruction limit is exceeded.
* Custom mlog instructions for performing runtime checks, logging messages, and reporting errors.
* Indication of stopped processors, processors executing a `wait`, and failed runtime checks.

![Screenshot of state indicated on processors](processors.png)

# Vars screen

The **Vars** screen has been enhanced:
* Additional processor variables (`@unit`, `@ipt`) are included.
* Numeric values are displayed in full precision. Integer values are clearly distinguished from decimal ones.
* Numeric values in the color range are formatted as color literals (e.g., `%e55454ff`), the color itself is presented using a glyph.
* Option to choose between formatting the numbers in hexadecimal or decimal base (including memory cell addresses).
* Option to sort variables by name or revert to their original order.
* Option to hide temporary variables and processor links.
* Commands to copy the variable list or the contents of the text buffer to the clipboard (in previous versions of this mod, these buttons have been included directly on the processor screen).

# Displaying the content of memory blocks

By tapping a memory block, a configuration button is now displayed. The button opens a **Memory** screen, similar to the **Vars** screen described above. All options relevant to the specifics of memory blocks are available. Furthermore, the **Edit** button allows you to clear the contents of memory blocks, copy them to the clipboard, and import them back.

The table kept on the clipboard may be modified (for example in a spreadsheet) and imported back into the memory block. The columns of the table are separated by tabs, and the address of a cell is always given in decimal. Strings are written as literals enclosed in quotes, with the characters which would either be lost or break the table encoded using escape sequences (`\n`, `\"`, `\\`, `\uXXXX`). Numbers may be entered in the decimal, color, hexadecimal or binary form, and values which are not finite are written and imported as `null`, as they are not representable in memory blocks. Lines which cannot be parsed are reported and skipped, and the memory block is only changed once the whole table has been parsed. Values having no representation in the text form (references to units, buildings and contents) are exported as well, but cannot be restored.

# Custom instructions

The custom instructions are used by [Mindcode](https://github.com/cardillan/mindcode) to provide debugging support or perform runtime checks. They can be used by other compilers too or by a manually written mlog.

When an assertion fails, the program execution stops at the given instruction, and an accompanying message is displayed above the processor.

## Instruction `assertbounds` 

This is a complex instruction, most useful to verify the value of a variable used as an index into an array is within bounds. Using out-of-bounds index for internal arrays can cause erratic, hard-to-diagnose behavior. The instruction takes these parameters:

* `type`: keyword. Specifies the required type of the value being tested: 
  * `any`: any value is allowed,
  * `notNull`: any value except `null` is allowed,
  * `decimal`: a numerical value is required,
  * `integer`: an integer value is required,
  * `multiple`: an integer value that is a specified multiple is required. 
* `multiple`: the required multiple in case `type` is `multiple`.
* `min`: the minimum allowed value. Corresponds to the `{1}` message placeholder.
* `opMin`: one of `lessThan` or `lessThanEq`, specifies whether the minimum value is included in the allowed range. Corresponds to the `{4}` message placeholder.
* `value`: the value being tested. Corresponds to the `{2}` message placeholder.
* `opMax`: one of `lessThan` or `lessThanEq`, specifies whether the maximum value is included in the allowed range. Corresponds to the `{5}` message placeholder.
* `max`: the maximum allowed value. Corresponds to the `{3}` message placeholder.
* `message`: the error message to display in case the assertion fails. The message may contain placeholders in the form `{1}` to `{5}`, corresponding to the values and instruction parameters described above. If the message is not a string or is an empty string, a default message is displayed.

## Instruction `assertequals` 

This instruction compares an actual value to an expected value and displays the given message if they are not equal. The instruction takes these parameters:

* `expected`: the expected value.
* `actual`: the actual value.
* `message`: the error message to display in case the assertion fails. The message may contain placeholders in the form `{1}` and `{2}` for the expected and actual values. If the message is not a string or is an empty string, a default message is displayed.

The values are compared using the `strictEqual` mlog operator.

## Instruction `asserttype`

This instruction compares the runtime data type to an expected value and displays the given message when the value is not of the expected type. The instruction takes these parameters:

* `expectedType`: the expected data type. Supported data types are:
  * `number`
  * `string`
  * `content`
  * `item`
  * `block`
  * `bulletType`
  * `liquid`
  * `statusEffect`
  * `unitType`
  * `weather`
  * `team`
  * `unitCommand`
  * `unitStance`
  * `unit`
  * `building`
  * `processor`
  * `memory`
  * `message`
  * `display`
  * `canvas`
  * `property`
  * `readable`
  * `writable`
  * `senseable`
* `actualValue`: the value being tested.
* `message`: the error message to display in case the assertion fails. The message may contain placeholders in the form `{1}` and `{2}` for the expected and actual values. If the message is not a string or is an empty string, a default message is displayed.

> [!NOTE]
> This instruction is still being developed and may not work as expected for some combinations of parameters. 

## Instruction `assertflush`

This instruction must be used at the beginning of a code section which generates text into the text buffer (using any of the printing instructions, `print`, `printchar` or `format`). The `assertprints` instruction is then used to compare the output generted by the program to an expected string. The instruction takes these parameters:

* `position`: output variable receiving the current position in the text buffer.

## Instruction `assertprints` 

This instruction compares the output generated by the program to an expected string. The instruction takes these parameters:

* `position`: the position in the text buffer at the beginning of the tested code (must be the variable used by the `assertflush` instruction).
* `expected`: the expected string.
* `message`: the error message to display in case the assertion fails. The message may contain placeholders in the form `{1}` and `{2}` for the expected and actual values. If the message is not a string or is an empty string, a default message is displayed.

When the instruction finishes, the text buffer is restored to the state of the previous `assertflush` instruction.

> [!NOTE]
> The content of the text buffer is not saved into the map file. Therefore, the `asssertprint` instruction may spuriously fail after a game is loaded from a save file. 

## Instruction `breakpoint`

This instruction comes with a condition (just like the `jump` instruction). When the condition is met, the game is paused in the exact state in which the breakpoint occurred and the processor with the breakpoint is centered on screen. No other instructions in the current processor or any other processors are executed after the breakpoint is hit, and all logic variables in all processors and contents of all memory cells should remain unchanged. However, it is possible that units and buildings do change their own state or the state of other units/buildings after the breakpoint hits, but before the game is properly paused.

Breakpoints are ignored in a multiplayer game.

> [!NOTE]
> After unpausing the game, the execution continues as usual, but the order of instruction execution may be altered due to the processors being manipulated. Schematics depending on two or more processors executing in lockstep (e.g., subframe) may be affected.

## Instruction `error`

This instruction displays an error message and stops the program execution. The instruction takes these parameters:

* `message`: the error message to display.
* `p1` .. `p9`: additional parameters to display in the error message.

If the message contains placeholders in the form `{1}` to `{9}`, they are replaced by the corresponding parameters. If there are other parameters not used by the message, whose value is not the literal `null`, they are appended to the message one by one. String values are enclosed in quotes in this case. Numeric values in the color range are formatted as color literals (e.g., %e55454ff`).

## Instruction `log`

This instruction writes a message into the game's log file. It is up to the user to avoid writing too many error messages into the file. The instruction takes these parameters:

* `level`: the log level of the message, one of `debug`, `info`, `warning`, or `err`.
* `message`: the error message to display.
* `p1` .. `p9`: additional parameters to display in the error message.

If the message contains placeholders in the form `{1}` to `{9}`, they are replaced by the corresponding parameters. If there are other parameters not used by the message, whose value is not the literal `null`, they are appended to the message one by one. String values are enclosed in quotes in this case. Numeric values in the color range are formatted as color literals (e.g., `%e55454ff`).

# Settings

## Disable breakpoints

Disables breakpoints in processors: the execution continues on the next instruction without pausing.

## Breakpoint on failed assertions

When active, a failed assertion is handled in the same way as a breakpoint and the execution then continues on the next instruction. If breakpoints are disabled, the assertions are completely ignored.

## Detach camera on breakpoint

When a breakpoint hits, the camera is detached so that the processor remains in view. The original state of the camera is restored when the game is unpaused. In case detaching the camera causes some problems, it can be disabled. 

## Instruction limit

Allows increasing the instruction limit in logic processors. The increased limit may be used to add debugging code to your program, which otherwise wouldn't fit due to the standard instruction limit being exceeded.

## Show waits longer than (sec)

Sets the minimal wait time specified in the `wait` instruction parameter which causes the wait to be indicated on processors. It is possible to completely hide the indication by setting the value to `0`. 

## Processor updates per tick

To indicate the stopped and waiting processors, the mod needs to inspect the state of all processors on the map periodically. At most the given number of processors gets inspected in each tick. If there are a lot of processors on the map and the performance suffers, reducing this value may help.   

## Visual warning

Governs the use of visual effects on the map itself to draw attention to stopped or failed processors. Effects can be turned off completely, performed just once when the processor is stopped or failed, or performed periodically with a given interval. 
