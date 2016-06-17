# Transformer tool for the PEACE website

## Actions

- _merge_: merges two *.json* files or two *tests* folder of a default betsy run.
    - arguments:
        - path to the old path
        - path to the new path
    - tests with the same featureID and engineID from the new json will replace same tests with the old json

- _copyFiles_: copies the engine-dependent, engine-independent or log files of the appropriate *.json* file. Keeps a copy (original-tests-engine-dependent.json
    - arguments:
        - path to the tests-engine-dependent.json to copy ( copyFiles <file/to/>/tests-engine-dependent.json )
    - example: copyFiles <path/to/>tests-engine-dependent.json

- _mergeDocker_: merges the results of a betsy docker run
    - arguments:
        - path to the root of the *results* directory of the betsy docker run
    - example: mergeDocker </path/to>/results