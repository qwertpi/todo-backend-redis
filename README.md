# todo-backend-redis
My implementation of the [Todo-Backend specification](https://todobackend.com/) with Redis as the data store and Scala as the web server (http4s) and test suite (MUnit)

## Copyright
Copyright © 2024  Rory Sharp All rights reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

You should have received a copy of the GNU General Public License
along with this program.  If you have not received this, see <http://www.gnu.org/licenses/gpl-3.0.html>.

For a (non-legally binding) summary of the license see https://tldrlegal.com/license/gnu-general-public-license-v3-(gpl-3)

## Installation
1. Install sbt using [coursier](https://get-coursier.io/docs/cli-installation)
2. `apt-get install redis`
3. `redis-cli CONFIG SET appendonly yes appendfsync everysec save ""`
4. `redis-cli CONFIG REWRITE`

## Usage
* To run tests: `sbt test`
* To run server: `sbt run`
