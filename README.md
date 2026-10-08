Undertow
========
Undertow is a Java web server based on non-blocking IO. It consists of a few different parts:

- A core HTTP server that supports both blocking and non-blocking IO
- A Servlet 4.0/5.0 implementation
- A JSR-356/Jakarta 2.0 compliant Web Socket implementation

Website: http://undertow.io

Issues: https://issues.redhat.com/projects/UNDERTOW

Project Lead: Flavia Rainone <frainone@redhat.com>

Undertow Dev Group: https://groups.google.com/g/undertow-dev/
(you can access archived discussions of old undertow-dev mail list [here](http://lists.jboss.org/mailman/listinfo/undertow-dev))

Zulip Chat: https://wildfly.zulipchat.com stream [#undertow](https://wildfly.zulipchat.com/#narrow/stream/174183-undertow)

Request body size and blocking read timeout
-------------------------------------------

When `MAX_ENTITY_SIZE` is not set, a request body is limited to 2MB (2097152 bytes). Multipart parsing uses `MULTIPART_MAX_ENTITY_SIZE`, which also defaults to 2MB when the request does not already have a positive entity-size limit. A blocking read of the request body waits at most 10 minutes (600000 milliseconds) when neither `READ_TIMEOUT` nor `IDLE_TIMEOUT` is set. A configured value of 0 or less removes that limit. If one of those timeouts is positive, the blocking read uses the smaller positive value, so both have to be 0 or less for a read with no timeout.

Embedded server:

```java
Undertow.builder()
        .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, -1L)
        .setServerOption(UndertowOptions.MULTIPART_MAX_ENTITY_SIZE, -1L)
        .setServerOption(UndertowOptions.IDLE_TIMEOUT, 0)
        .setSocketOption(Options.READ_TIMEOUT, 0)
        .build();
```

`MAX_ENTITY_SIZE` is the maximum request body size. `MULTIPART_MAX_ENTITY_SIZE` is the multipart body size used when the request has no positive entity-size limit. `Options.READ_TIMEOUT` is the socket read timeout, in milliseconds. `IDLE_TIMEOUT` is the connection idle timeout, in milliseconds.

WildFly 26:

WildFly 26 sets these on each listener in the `undertow` subsystem (`urn:jboss:domain:undertow:12.0`). `max-post-size` is `MAX_ENTITY_SIZE`. The listener default is 10MB, which replaces Undertow's 2MB default. The management model accepts 0 as the smallest value, and Undertow treats 0 as no body-size limit. `read-timeout` is the socket `READ_TIMEOUT`, in milliseconds. It is unset by default, so blocking reads use the 10 minute timeout until it is set to 0. `IDLE_TIMEOUT` is not a WildFly 26 listener attribute. With it left unset, `read-timeout="0"` disables the blocking read timeout.

Add both attributes to every `http-listener`, `https-listener`, and `ajp-listener` that should accept an unlimited body, then reload:

```xml
<http-listener name="default" socket-binding="http" redirect-socket="https" enable-http2="true" max-post-size="0" read-timeout="0"/>
<https-listener name="https" socket-binding="https" ssl-context="applicationSSC" enable-http2="true" max-post-size="0" read-timeout="0"/>
```

```
/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=max-post-size,value=0)
/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=read-timeout,value=0)
/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=max-post-size,value=0)
/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=read-timeout,value=0)
reload
```

WildFly 26 has no management attribute for `MULTIPART_MAX_ENTITY_SIZE`. With `max-post-size` set to 0, multipart parsing still applies the 2MB multipart default unless that Undertow option is also 0 or negative. `no-request-timeout` closes a connection that has been idle with no request in progress. The blocking read timeout is `read-timeout`.

Contributing to Undertow - PR Review Process
--------------------------------------------

Bug fixes and documentation improvements are welcome! If you want to contribute and are not sure where to start, I suggest you have a look at our [Jira project](https://issues.redhat.com/projects/UNDERTOW "Undertow Jira") and get in touch with us via [Zulip chat](https://wildfly.zulipchat.com/#narrow/stream/174183-undertow "#undertow").

PRs must be submitted to master branch (soon to be [renamed to main](https://issues.redhat.com/browse/UNDERTOW-2043)) and they should:
- state clearly what they do
- point to associated Jira
- contain a test case, unless existing tests already verify the code added by the PR
- have a license header in all new files, with current year’s number
- pass CI (except for known failures, we are working on fixing those, tracked by [UNDERTOW-1523](https://issues.redhat.com/browse/UNDERTOW-1523))

If your PR is incomplete, the reviewer might request you add the missing bits or add them for you if that is simple enough.

PR reviewers will take into account the following aspects when reviewing your PR:
- correctness: the code must be correct
- performance impact: if there are negative performance impacts in the code, careful consideration must be taken whereas the impact could be eliminated and, in case it cannot, if the new code should be accepted
- code style: keep your code style consistent with the classes you are editing, such as variable names, ordering of methods, etc
- scope of the fix: this is a very important factor. Sometimes, the fix should be applied to a broader range of classes, such as a bug that repeats itself in other parts of the code. Other times, the PR solves a bug only partially, because the bug has a broader impact than initially evaluated.
- is the proposed fix the best approach for the Jira at hand?
- backwards compatibility: we must prevent any PR that breaks compatibility with previous versions
- security impact: it is critical to evaluate if the PR has any sort of security impact, preventing the addition of exploitable flaws.

Your PR will be classified by the reviewer with one or more of the following labels: **bug fix**, **enhancement**, **new feature/API change**, and **dependency upgrade**.

Besides the classifications labels, a series of labels are going to be added to your PR while it is under review. This is the full list of labels and what they mean:
- **waiting CI check**  PR is ready to be merged, but we are waiting for CI results. The use of this label is optional.
- **waiting PR update**  reviewer has requested changes to the PR
- **failed CI**  a new failure was introduced to CI
- **question** reviewer has asked one or more questions to the contributor, so the PR can be better assessed
- **under verification**  reviewer will perform some extra verifications before giving their feedback (usually this means running reproducers, reviewing specs, and the like)
- **waiting peer review** PR has been reviewed but is waiting on a second review before being merged  (as the changes affects core classes or adds a new feature)
- **next release** PR is in the payload of the next release

Notifying Security Relevant Bugs
--------------------------------

If you find a bug that has a security impact, please notify us sending an email to Red Hat SecAlert <secalert@redhat.com> with a copy to Flavia Rainone <frainone@redhat.com>. This will ensure the bug is properly handled without causing unnecessary negative impacts for the Undertow's user base.

You can find more information about the security procedures at [this page](https://access.redhat.com/security/team/contact "Security Contacts and Procedures").
