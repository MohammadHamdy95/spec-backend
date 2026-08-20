# API contract

[`openapi.yaml`](openapi.yaml) describes what the service actually does.

Unlike tiny and paste, this one was written **from** the implementation
rather than before it — the service was built first. It is kept honest by
being checked against `SpecController` and the DTOs, not by being the thing
they were generated from.

## Hosted copy

The contract is published on the service it describes:

**https://spec.hamdy.app/7ljE0iyP5y**

That is not a gimmick — it is the cheapest end-to-end test there is. If a
revision of this file parses, renders in both Scalar and Swagger UI, and
diffs cleanly against the previous one, the whole pipeline works.

## Publishing a revision

The edit token was returned once at creation and is not in this repo. It
lives in the maintainer's password manager; without it the hosted copy can
still be read by anyone, but revised by nobody.

```bash
python3 -c 'import json;print(json.dumps({"body":open("api/openapi.yaml").read(),"note":"what changed"}))' \
  | curl -s -X PUT https://spec.hamdy.app/v1/specs/7ljE0iyP5y \
      -H 'Content-Type: application/json' \
      -H "X-Edit-Token: $SPEC_EDIT_TOKEN" \
      --data-binary @-
```

A 422 means the document is not a usable API description; the response
carries the parser's own messages in an `errors` array. Then compare it
against the previous revision:

```bash
curl -s 'https://spec.hamdy.app/v1/specs/7ljE0iyP5y/diff' | python3 -m json.tool
```
