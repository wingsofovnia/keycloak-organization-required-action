# keycloak-organization-required-action
A Keycloak required action that enforces the creation of an KC26 Organization during user registration —
unless the user was invited via a link.

<p align="center">
    <img
        alt="Required Action in Action (Animation)"
        src="https://github.com/user-attachments/assets/fc343d08-400d-4dc5-918a-90a1c8595028"
        width="500px"
    />
</p>

## Motivation
Keycloak 26 introduced a new feature called **Organization**, which simplifies the management of multi-tenant environments.

However, the primary use case targeted by Organizations is administrative isolation within a single realm, with
organizations being created manually by realm admins. This makes it difficult to support SaaS-style multi-tenancy
where tenants self-register.

To support this use case, this extension introduces a custom [required action](https://www.keycloak.org/docs/latest/server_admin/index.html#core-concepts-and-terms:~:text=client%20requesting%20authentication.-,required%20actions,update%20password%20required%20action%20would%20be%20set%20for%20all%20these%20users.,-authentication%20flows)
that enforces organization creation during user registration — ensuring every user is tied to an organization without manual
admin intervention.

## Features
* Ensures every user is assigned to an organization.
* Not applied if the user is invited via a link (handled by standard [`RegistrationUserCreation`](https://github.com/keycloak/keycloak/blob/faea1d6595bd3a33643088cd6d8a1feef45c5417/services/src/main/java/org/keycloak/authentication/forms/RegistrationUserCreation.java#L337-L350)).
* Ignores users with preconfigured roles (realm `admin` by default).
* Supports generating random organization domains to satisfy model constraints as it doesn't make much sense
  for self-registered tenants.
* Appends a `?query=true` flag to the redirect URL to allow custom handling.
* Supports assigning users as either managed or unmanaged members.
* Customizable help text.

## Installation
* Build `./gradlew :jar` or take latest from [Releases](https://github.com/wingsofovnia/keycloak-organization-required-action/releases).
* Put the jar to `${kc.home.dir}/providers` folder.
* Run `${kc.home.dir}/bin/kc.sh build` to complete the installation.
* Restart.
* Enable the required action in Keycloak Administration Console:
  * `Authentication` in `Configure` section -> `Required Actions` tab.
  * Enable `Require Create & Join Organization`.
  * Click ⚙️ to customize the default configuration.

## License
MIT License

Copyright (c) 2025 Illia Ovchynnikov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
