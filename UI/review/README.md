# Dark UI/UX Review Notes

`UI/review/*` 是审查输入和历史反馈记录，不是当前实现合同。执行 dark UI/UX PR 时，权威顺序固定为：

1. [../PLAN.md](../PLAN.md)
2. [../pr/README.md](../pr/README.md)
3. 对应 [../pr/](../pr/) 下的 PR 文档
4. 当前代码、focused tests、golden/manual evidence

如果历史 review 行提到 `AsciiRenderer`、`AsciiRenderModel`、`AsciiRenderModelTest`、ASCII fallback 或 ASCII manifest 字段，这些反馈已经被当前 Tile-only 合同 supersede：client ASCII fallback / debug renderer 已删除，不得作为后续修复项重新实现。正确处理方式是把有价值的意图吸收到 Tile dark UI、manifest fallback / missing visual、viewport、renderer 拆分、overlay layer 或 focused tests 中。

吸收 review 反馈时，必须先验真当前代码和 PR 文档，再把仍然有效的行动项写入对应 PR 文档或实现；不要直接按历史 review 表格执行。
