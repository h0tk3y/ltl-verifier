# ltl-verifier

## Usage

Download a ditribution from the [releases page](https://github.com/h0tk3y/ltl-verifier/releases).

From inside the distribution, run `ltl-verifier/bin/ltl-verifier` or `ltl-verifier/bin/ltl-verifier.bat` depending on your OS.
The command line interface is:

```
usage: [-h] --xml XML [--ltl-string LTL_STRING] [--ltl-file LTL_FILE]

required arguments:
  --xml XML                 Diagram XML file of the model


optional arguments:
  -h, --help                show this help message and exit

  --ltl-string LTL_STRING   LTL formula to verify

  --ltl-file LTL_FILE       File containing lines each representing an LTL
                            formula
```

## Input format

LTL formulas: see the grammar: [`LtlParser.kt`](https://github.com/h0tk3y/ltl-verifier/blob/master/src/main/kotlin/LtlParser.kt) and the examples from the run below.

Automata: see the example [`src/main/resources/automata/test1.xml`](https://github.com/h0tk3y/ltl-verifier/blob/master/src/test/resources/automata/test1.xml)

## Example

```
ltl-verifier --xml=test1.xml --ltl-file=test1.ltl
```

```
Formula: G(hal_init -> X(tim4_enable))
Correct.

Formula: G(pin_reset_s1 -> X(pin_reset_s2))
Correct.

Formula: G(pin_reset_s2 -> X(pin_reset_s3))
Correct.

Formula: G(hal_init -> F(tim4_enable))
Correct.

Formula: G(F(PRESTART))
Found counter-example:
Path:
Start
Start, tick
Start, tick, hal_init
Start, tick, tim4_enable
PRESTART
PRESTART, tick
PRESTART, tick, shell_deinit
PRESTART, tick, bq_deinit
PRESTART, tick, pin_reset_s1
PRESTART, tick, pin_reset_s2
PRESTART, tick, pin_reset_s3
PRESTART, tick, delay_5000
POWER_ON
POWER_ON, CHG
CPU_ON
CPU_ON, CHG
BAT_ONLY
BAT_ONLY, CHG
CPU_ON
CPU_ON, CHG
--- cycle:
BAT_ONLY
BAT_ONLY, CHG
CPU_ON
CPU_ON, CHG

Formula: G(SLEEP -> F(POWER_ON))
Correct.

Formula: G(PRESTART -> (PRESTART) U (POWER_ON))
Correct.

Formula: G((POWER_ON) & (CHG) -> (POWER_ON) U (FLASH))
Found counter-example:
Path:
Start
Start, tick
Start, tick, hal_init
Start, tick, tim4_enable
PRESTART
PRESTART, tick
PRESTART, tick, shell_deinit
PRESTART, tick, bq_deinit
PRESTART, tick, pin_reset_s1
PRESTART, tick, pin_reset_s2
PRESTART, tick, pin_reset_s3
PRESTART, tick, delay_5000
POWER_ON
POWER_ON, CHG
CPU_ON
--- cycle:
CPU_ON, OFF
PRESTART
PRESTART, tick
PRESTART, tick, shell_deinit
PRESTART, tick, bq_deinit
PRESTART, tick, pin_reset_s1
PRESTART, tick, pin_reset_s2
PRESTART, tick, pin_reset_s3
PRESTART, tick, delay_5000
POWER_ON
POWER_ON, CHG
CPU_ON
```
