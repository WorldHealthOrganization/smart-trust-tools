#!/usr/bin/env bash
# Build the viewer site and publish it to the gh-pages branch of this
# repository (litlfred/smart-trust-tools). Run by hand or by the agent; there
# is deliberately no GitHub workflow for it.
#
#   tools/publish_site.sh            publish to the gh-pages root
#   tools/publish_site.sh preview X  publish to gh-pages/branches/X
#
# Needs Python (pyyaml markdown rdflib cbor2 base45), Node.js, and push
# access to origin. Existing branches/ previews are kept on a root publish.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
remote="$(git -C "$root" remote get-url origin)"
case "$remote" in
  *litlfred/smart-trust-tools*) ;;
  *) echo "refusing to publish: origin is $remote, not litlfred/smart-trust-tools" >&2; exit 1 ;;
esac

target="."
if [ "${1:-}" = "preview" ]; then
  target="branches/${2:?preview needs a name}"
fi

sha="$(git -C "$root" rev-parse --short HEAD)"
ref="$(git -C "$root" rev-parse --abbrev-ref HEAD)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

SITE_GIT_REF="$ref" "$root/tools/build_site.sh" "$work/site"

if git -C "$root" ls-remote --exit-code --heads origin gh-pages >/dev/null; then
  git clone -q --depth 1 --branch gh-pages "$remote" "$work/pages"
else
  git init -q -b gh-pages "$work/pages"
  git -C "$work/pages" remote add origin "$remote"
fi

dest="$work/pages/$target"
if [ "$target" = "." ]; then
  find "$work/pages" -mindepth 1 -maxdepth 1 ! -name .git ! -name branches -exec rm -rf {} +
else
  rm -rf "$dest"
fi
mkdir -p "$dest"
cp -R "$work/site/." "$dest/"
rm -f "$dest/index.fragment.html"

git -C "$work/pages" add -A
if git -C "$work/pages" diff --cached --quiet; then
  echo "gh-pages already up to date"
  exit 0
fi
git -C "$work/pages" commit -q -m "Publish site from $ref@$sha to ${target}"
git -C "$work/pages" push -q origin gh-pages
echo "published $ref@$sha to gh-pages:${target}"
