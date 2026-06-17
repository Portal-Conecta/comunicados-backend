# TESTES QUE FUNCIONARAM JÁ

## Perfil SENAI
- Publicar comunicado -> ```POST /api/posts/publish```
- Listar comunicados -> ```GET /api/posts?page=0&size=20```
- Buscar por termo -> ```GET /api/posts?search=retirada```
- Buscar por ID -> ```GET /api/posts/{{postId}}```
- Atualizar comunicado -> ```PUT /api/posts/{{postId}}```
- Agendar comunicado -> ```POST /api/posts/schedule```
- Remover comunicado -> ```DELETE /api/posts/{{postId}}```
- Buscar por Filtro -> ```GET /api/posts?page=0&size=20&origin=SENAI&search=retirada```
- Buscar por Turma -> ```GET /api/posts?page=0&size=20&classId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa```

## Perfil WEG
- Publicar comunicado -> ```POST /api/posts/publish```
- Listar comunicados -> ```GET /api/posts?page=0&size=20```
- Buscar por termo -> ```GET /api/posts?search=retirada```
- Buscar por ID -> ```GET /api/posts/{{postId}}```
- Atualizar comunicado -> ```PUT /api/posts/{{postId}}```
- Agendar comunicado -> ```POST /api/posts/schedule```
- Remover comunicado -> ```DELETE /api/posts/{{postId}}```
- Buscar por Filtro -> ```GET /api/posts?page=0&size=20&origin=WEG&search=retirada```
- Buscar por Turma -> ```GET /api/posts?page=0&size=20&classId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa```
    
## Perfil STUDENT
- Publicar comunicado (NEGADO) -> ```POST /api/posts/publish```
- Listar comunicados -> ```GET /api/posts?page=0&size=20```
- Buscar por termo -> ```GET /api/posts?search=retirada```
- Buscar por ID -> ```GET /api/posts/{{postId}}```
- Atualizar comunicado (NEGADO) -> ```PUT /api/posts/{{postId}}```
- Agendar comunicado (NEGADO) -> ```POST /api/posts/schedule```
- Remover comunicado (NEGADO) -> ```DELETE /api/posts/{{postId}}```
- Buscar por Filtro -> ```GET /api/posts?page=0&size=20&origin=WEG&search=retirada```
- Buscar por Turma -> ```GET /api/posts?page=0&size=20&classId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa```

## Perfil TEACHER
- Publicar comunicado (APENAS PARA TURMAS ASSOCIADAS) -> ```POST /api/posts/publish```
- Listar comunicados -> ```GET /api/posts?page=0&size=20```
- Buscar por termo -> ```GET /api/posts?search=retirada```
- Buscar por ID -> ```GET /api/posts/{{postId}}```
- Atualizar comunicado (APENAS PUBLICADAS/AGENDADAS POR ELE MESMO) -> ```PUT /api/posts/{{postId}}```
- Agendar comunicado (APENAS PARA TURMAS ASSOCIADAS) -> ```POST /api/posts/schedule```
- Remover comunicado (APENAS PUBLICADAS/AGENDADAS POR ELE MESMO) -> ```DELETE /api/posts/{{postId}}```
- Buscar por Filtro -> ```GET /api/posts?page=0&size=20&origin=WEG&search=retirada```
- Buscar por Turma -> ```GET /api/posts?page=0&size=20&classId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa```