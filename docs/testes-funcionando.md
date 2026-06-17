# TESTES QUE FUNCIONARAM JÁ

## Perfil SENAI
- Publicar comunicado -> ```POST /api/posts/publish```
- Listar comunicados -> ```GET /api/posts?page=0&size=20```
- Buscar por termo -> ```GET /api/posts?search=retirada```
- Buscar por ID -> ```GET /api/posts/{{postId}}```
- Atualizar comunicado -> ```PUT /api/posts/{{postId}}```
- Agendar comunicado -> ```POST /api/posts/schedule```
- Remover comunicado -> ```DELETE /api/posts/{{postId}}```