{
  "schemaVersion": 1,
  "id": "{{PACK_ID}}",
  "version": "{{PACK_VERSION}}",
  "name": "{{PACK_NAME}}",
  "description": "{{PACK_DESC}} (由 ModCrafter 模组工坊在游戏内制作并导出)",
  "authors": [
    "{{PACK_AUTHOR}}"
  ],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": [
      "{{PACKAGE}}.PackMod"
    ],
    "client": [
      "{{PACKAGE}}.PackModClient"
    ]
  },
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": [">=1.21", "<1.21.2"],
    "java": ">=21",
    "fabric-api": "*"
  }
}
