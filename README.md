# YCB

YCB is a multi-dimensional configuration library that builds bundles from
resource files describing a variety of values.  The library allows applications
to configure themselves based on multiple dimensions describing locations,
languages, environments, etc.

## Overview

YCB allows applications to specify configurations that change in response to
specific environments. But, different than traditional systems in which this
environment is a single dimension (_e.g._ development _vs_ production), YCB
permits the specification of more orthogonal dimensions. Examples of dimensions
include:

 * Device: iPhone, Android, Desktop Browser.
 * Deployment: development, staging, production.
 * Language: en-US, es-ES, pt-BR
 * Location: North America, Asia, Europe

Each of these dimensions has a name (like  "Language"), and a set of possible
values ("en-US", "es-ES", "pt-BR"). Also, dimensions can be immutable per
application instance (like "Deployment"), or change dynamically
at at different context (like "Device" in a HTTP request).

A _realization_ of a set of dimensions is called "context", _e.g._ of a context
for the previous dimensions specification can be something like:

 * Device: Android
 * Deployment: development
 * Language: *
 * Location: Asia

In this example we illustrated the use of the special value `*` - also "any" or
"unspecified" - this means that "Language" not specified. This issues the
selection of default values for when language dimension matters in a
configuration. The case when all dimensions in a contexts are `*` is called "master".

Dimension values hierarchical, `*` is the root in the value tree (_i.e._ all
values are descendant of `*`), and the user can specify other value hierarchies.
The `dimensions.yml` file is the place to specify the dimensions and it's value
tree, example:

```yaml
- dimensions:
    -
         deployment:
            development:
            staging:
            production:
              east-coast:
              west-coast:
    -
        user_type:
            free:
            premium:
    -
        locale:
            en:
                en-AU:
                en-BG:
            es:
                es-AR:
                es-BO:
```

In the example `en-AU` is descendant of `en`, this means that all configuration
that applies for `en` also applies for `en-AU` - same thing for `west-coast` and
`production` in `deployment`, for example. The `*` value is implicitly the root
value, so that all configuration that applies to `*`, also applies to all others.

The way configuration is specified is in YAML or JSON files containing one or
more "bundles". Each bundle is composed of a context and a delta. The configuration
delta get's used if the configuration context matches the bundle contexts, example:

```yaml

# omitted dimensions defaults to "*",
# therefore this is the "master"
- settings: {}
  feature_x:
      enabled : false
      constant_alpha: 0.8

- settings: {user_type: premium}
  feature_x:
      enabled: true

- settings: {deployment: development}
    feature_x:
      constant_alpha: .99
```

This example has three bundles, "settings" is a reserved key used to specify the
bundle's context; everything else is bundle's delta.

The final configuration (called "projection") is a composition from the most
generic to the most specific delta (_i.e._ from the delta that specify dimension
values higher in the value tree to the ones that specify deeper values).

If the user requests a projection with the "master" context, she will get exactly
the delta specified in the "master" bundle (first one), because no other bundle
matches the context.

If the user requests a projection if `user_type` = `premium`, then YCB will start
from the "master" (because is more generic, and matches the contexts,
because `premium` is descendant of `*`), and then apply the second bundle. In
this case the `feature_x.enabled` gets overwritten with `true`, so the final
"projected" configuration will be:

```yaml
feature_x:
    enabled : true
    constant_alpha: 0.8
```

This behavior is analogous for when the contexts is `deployment` = `development`.

On a more complex example, if the context has both `user_type` = `premium` and
`deployment` = `development`, then YCB will start from master and apply both
bundles. When the specificity of the context is ambiguous, the the dimension
is used to disambiguate, by following the order of definition of the dimension.
In this case, `environment` was defined first, so the third bundle is applied
before the second one, resulting in the final projected configuration:

```yaml
feature_x:
    enabled : true
    constant_alpha: 0.99
```

This covers briefly the capabilities of YCB. The tests are most likely the best
place to get to know the API.

## License

Code licensed under the BSD license.  See LICENSE file for terms.
