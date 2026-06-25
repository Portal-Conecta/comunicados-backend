## Contexto

Tags classificam comunicados e permitem filtros no front-end. Após a auto-vinculação por destino (#110), a listagem precisava suportar filtro por tag. A issue [#150](https://github.com/Portal-Conecta/comunicados-backend/issues/150) cobre esse gap, alinhada ao contrato em `docs/tags-por-eventos.md` (§4 — Uso em filtros).

## O que muda

`GET /api/posts` passa a aceitar `tagId` (UUID) e `tagIds` (lista) para retornar apenas comunicados vinculados à(s) tag(s) informada(s). O filtro combina com os existentes (`origin`, `classId`, `search`, intervalo de publicação) e respeita as regras de visibilidade por perfil. Múltiplas tags usam semântica **OR**; tag inexistente retorna **lista vazia** (200), sem 404.

## Issue relacionada

Closes #150

## Como testar

1. Subir a aplicação com profile `dev` e autenticar com JWT válido.
2. Publicar ou agendar um comunicado com destino que gere auto-vinculação de tag (ex.: `CLASS` ou `GENERAL`).
3. Obter o UUID da tag via `GET /api/tags` ou `GET /api/posts/{id}/tags`.
4. Chamar `GET /api/posts?tagId={uuid}` e verificar que só retornam comunicados vinculados à tag.
5. Chamar `GET /api/posts?tagIds={uuid1}&tagIds={uuid2}` e confirmar comunicados com **qualquer** uma das tags (OR).
6. Chamar com UUID de tag inexistente e confirmar resposta 200 com `items: []`.
7. Combinar filtros: `GET /api/posts?tagId={uuid}&origin=SENAI&search=retirada`.
8. Rodar testes: `.\mvnw test` (requer JDK 21+).

## Tipo de mudança

- [x] Nova feature
- [ ] Correção de bug
- [ ] Refatoração (sem mudança de comportamento)
- [ ] Documentação
- [ ] Infraestrutura / config / build
- [ ] Outro: ____________________

## Checklist do autor

- [x] Código segue convenções definidas em CONTRIBUTING.md
- [x] Validei localmente que a aplicação compila/gera build sem erros (quando aplicável)
- [x] Verifiquei que não há erros de análise estática ou alertas relevantes no código (quando aplicável)
- [ ] Testei manualmente os cenários principais
- [x] Documentação atualizada (se aplicável)
- [x] Não introduzi dependências novas sem alinhamento prévio

## Algo a mudar na documentação?

OpenAPI do endpoint `GET /api/posts` foi atualizado no controller. O contrato em `docs/tags-por-eventos.md` (§4) já mencionava `tagId`/`tagIds`; a decisão de OR para múltiplas tags e lista vazia para tag inexistente está documentada nas anotações `@Schema` de `PostFilterRequest`.

---

## Notas pro revisor

- **Semântica OR** para `tagIds`: comunicado aparece se tiver vínculo com qualquer tag da lista (padrão usual de filtro por categoria).
- **Tag inexistente → lista vazia**: evita 404 em parâmetro de filtro; consistente com outros filtros de listagem.
- **`tagId` + `tagIds` juntos**: mesclados e deduplicados via `PostFilterRequest.resolvedTagIds()`.
- **Performance**: subquery com `EXISTS` em `announcement_tag`, mesmo padrão de `matchesSearch` / `tagNameMatches`.
- Arquivos principais: `PostFilterRequest`, `AnnouncementSpecifications.hasAnyTag()`, `ListAnnouncementsUseCase`, `AnnouncementController`.

---

## Checklist do Desenvolvedor (Antes de solicitar Review)

- [ ] Minha branch está atualizada com a `develop` mais recente.
- [x] O código compila localmente sem erros e sem alertas no terminal.
- [ ] O projeto roda perfeitamente (não quebrei a aplicação).
- [x] Não subi arquivos sensíveis ou inúteis (`.env`, pastas de build).
- [ ] Meus commits seguem o Conventional Commits.
- [ ] A issue correspondente no board foi movida para "In Review".
