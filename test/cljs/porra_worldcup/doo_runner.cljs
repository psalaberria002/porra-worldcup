(ns porra-worldcup.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [porra-worldcup.core-test]))

(doo-tests 'porra-worldcup.core-test)

