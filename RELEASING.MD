Datawave Ingest Service uses a github action to perfom the maven/helm version update and create release tags.

The application follows Semantiv Versioning ([semver.org](semver.org)) for the jar packages as well as the helm charts. The helm chart versions and application jars are kept in sync.

- MAJOR version is incremeted with incompatible API Changes
- MINOR version is incremented when changes are made in a backwards compatible manner
- PATCH version is incremented when backwards compatible bug fixes are added


# GitHub Release Process

## Triggering a release
Once all the changes are merged into the master branch and all CI checks are passing the the application is ready for release. 

**GitHub Release action can only be triggered by users in the `RELEASE_USERS` variable list**

1) Navigate to the Release Action under the github actions tab: [RELEASE ACTION](github.com/NationalSecurityAgency/datawave-ingst-services/actions/workflows/release.yml)
2) Click the `Run Workflow` button
3) For Major and Minor releases run workflow from `master` branch
4) Fill in Release version and next development version. Usually a minor bump with `-SNAPSHOT` appended at the end.
5) Click Run Workflow


## GitHub Release Notes
- The release job does not do any build verification or checks
- The job does not publish any artifacts (jars, dockerfiles, helm)
- Git tags are created with a `v` prefix
- The Job does not create a github release


# GitLab Mirror Artifact Publishing
After the github action has run and created a release tag then the versioned artifacts can be created and published for the closed network repository. This is a manual process for now but automatic publishing can be added in the future.

## Manual Publishing an artifact
1) Navigate to the gitlab mirror repository
2) Under the CI/CD tab on the side click on Pipelines
3) Click the `Run pipline` button at the top
4) Under the branch option select the release tag `v*.*.*`
5) Input variable with the key `PUBLISH` and the value `TRUE`
6) Run Pipeline

## Gitlab Publishing Notes
- Only publishes helm packages for now
- Minimal build validation
- Required gitlab CI variables:
    - `CI_IMAGE`: Default image that is used for the gitlab executer
    - `CI_BUILD_IMAGE`: Image for the java build stage of gitlab CI
    - `ARTIFACTORY_REPOSITORY`: Artifactory Repository to publish helm charts 
    - `ARTIFACTORY_KEY`: Secret key for publish authorization