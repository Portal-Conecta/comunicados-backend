# Postman — Comunicados Backend

## Importar

1. Abra o Postman → **Import**
2. Selecione os dois arquivos:
   - `comunicados-backend.postman_collection.json`
   - `comunicados-local.postman_environment.json`
3. Ative o environment **Comunicados Local**

## Configurar token

1. Gere JWT no [jwt.io](https://jwt.io):
   - Algorithm: **HS256**
   - Marque **secret is base64 encoded**
   - Secret: `ZGV2LXNlY3JldC1rZXktMzItYnl0ZXMtbWluaW11bS1mb3ItaHMyNTY=`
   - Payload SENAI:
     ```json
     {
       "sub": "11111111-1111-1111-1111-111111111111",
       "userType": "SENAI",
       "classes": [],
       "exp": 1893456000
     }
     ```
2. Cole o token em `token` e `tokenSenai` no environment

## Ordem sugerida

```
00 - Saúde → Health check
01 - Auth → Listar sem token → Listar com token
02 - Comunicados → Publicar → Listar → Buscar → GET by ID → Atualizar → Agendar → Remover
05 - Tags (após inserir tag no H2) → Listar → Vincular → Listar do post → Desvincular
03 - Escopo (tokens Teacher/Student)
04 - Busca
```

## Tag de teste (H2 Console)

`http://localhost:8083/h2-console`

```sql
INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
VALUES (
  '66666666-6666-6666-6666-666666666666',
  'MI78 - Manhã',
  'CLASS',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);
```

Guia completo: [docs/postman-testing-guide.md](../docs/postman-testing-guide.md)
