# Users CLI

A Python command-line tool for managing users.

Requires project environment variables to be in scope
(see [template file](../../.env.template)).

## Features

Making users based on given username and password via command `add`.

It's possible to make user admin (by passing `--admin`), though
it's recommended to use it for initial admin user only. Prefer
project API to add new ones.

Users can be granted permissions with `grant` command that accepts username
and permission string (`namespace.name`).