# darktower

A clojure-based, multi-player implementation of the classic Milton Bradley board game.

## Overview

Here's another of the multi-player games I've been working on. This is the one I usually default to when I want to 
get a slightly larger/more complex set of game rules implemented. For more information on the game itself, and to have a
look at other implementations by other folks, head to [http://well-of-souls.com/tower/index.html](http://well-of-souls.com/tower/index.html).

## Setup

To get an interactive development environment run:

    lein run

and

    lein figwheel

then open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

## License

Copyright © 2016 rustybentley.com 

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
