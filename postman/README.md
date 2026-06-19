# Postman — Comunicados + Core

Coleções para testar a API de comunicados (`localhost:8083`) com autenticação real via **core-backend** (`localhost:8080`).

## Arquivos

| Arquivo | Descrição |
|---------|-----------|
| `comunicados-local.postman_environment.json` | Variáveis (URLs, tokens, classId, postId) |
| `Core - Login.postman_collection.json` | Login admin, criar usuários, turma e tokens |
| `Comunicados - SENAI.postman_collection.json` | 10 endpoints como SENAI |
| `Comunicados - WEG.postman_collection.json` | 10 endpoints como WEG |
| `Comunicados - TEACHER.postman_collection.json` | 10 endpoints como TEACHER |
| `Comunicados - REPRESENTATIVE.postman_collection.json` | 10 endpoints como REPRESENTATIVE |
| `Comunicados - STUDENT.postman_collection.json` | 10 endpoints como STUDENT |
| `generate_collections.py` | Script gerador (rode após alterar templates) |

> A coleção antiga `Comunicados Backend.postman_collection.json` foi substituída pelas coleções por perfil.

## Importar

1. Postman → **Import**
2. Selecione o environment + todas as coleções acima
3. Ative o environment **Comunicados Local (Core)**

## Pré-requisitos

- **Core** rodando em `http://localhost:8080`
- **Comunicados** rodando em `http://localhost:8083`
- Mesmo `JWT_SECRET` nos dois projetos (padrão em dev)

## Fluxo completo

### 1. Core — Login (Collection Runner)

Execute a coleção **Core - Login** na ordem das pastas:

```
00 - Auth          → Login admin (admin@portal.test / 123456)
01 - Criar usuários → SENAI, WEG, TEACHER, REPRESENTATIVE, STUDENT
02 - Login por perfil → salva userIds e tokens
03 - Turma         → curso, turma, vínculos
04 - Re-login      → TEACHER/REP/STUDENT (JWT com classes)
```

**Domínios de e-mail obrigatórios (Core):**

| Perfil | Domínio |
|--------|---------|
| SENAI | `@sc.senai.br` |
| WEG | `@weg.net` |
| TEACHER | `@edu.sc.senai.br` |
| STUDENT / REPRESENTATIVE | `@estudante.sesisenai.org.br` |

### 2. Comunicados — por perfil

Abra a coleção do perfil desejado e execute na ordem:

```
01 Publicar → 02 Agendar → 05 Listar → 07 Busca → 08 Por ID
→ 09 Fixar → 03 Editar → 10 Desafixar → 04 Deletar
```

Cada request tem testes automáticos (status esperado por perfil).

## Comportamento esperado por perfil

| Ação | SENAI | WEG | TEACHER | REP | STUDENT |
|------|-------|-----|---------|-----|---------|
| Publicar / Agendar | 201 | 201 | 201* | 201* | 403 |
| Editar / Deletar (próprio) | 200/204 | 200/204 | 200/204 | 200/204 | 403 |
| Listar / Buscar / Por ID | 200 | 200 | 200 | 200 | 200 |
| Fixar / Desafixar | 200 | 200 | 403 | 403 | 403 |

\* TEACHER e REPRESENTATIVE precisam estar vinculados à turma (`{{classId}}`) via Core.

## Variáveis do environment

| Variável | Uso |
|----------|-----|
| `coreBaseUrl` | Hub (8080) |
| `baseUrl` | Comunicados (8083) |
| `tokenSenai`, `tokenWeg`, … | JWT por perfil |
| `classId` | Turma criada no Core |
| `postId` | Preenchido ao publicar |
| `scheduledPostId` | Preenchido ao agendar |
| `scheduledFor` | Data ISO futura para agendamento |

## Regenerar coleções

```bash
cd postman
python generate_collections.py
```

Guia detalhado: [docs/postman-testing-guide.md](../docs/postman-testing-guide.md)
