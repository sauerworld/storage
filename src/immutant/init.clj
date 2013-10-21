(ns immutant.init
  (:require [immutant.util :refer (at-exit)]
            [sauerworld.storage.core :refer (stop)]))

(at-exit (stop))
