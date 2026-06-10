# Guia de documentação Swagger/OpenAPI

## Objetivo

Este guia define o padrão de documentação Swagger/OpenAPI para o módulo de Comunicados. A documentação deve descrever o contrato HTTP externo da API: rotas, métodos, parâmetros, payloads, respostas, erros esperados, autenticação e exemplos úteis para frontend, backend, testes e integrações.

Swagger não deve documentar a implementação interna. Não descreva ordem de chamadas de repositories, entidades JPA, tabelas, consultas SQL, regras internas de use cases ou detalhes de banco de dados.

## Ferramenta usada

O projeto usa Springdoc OpenAPI com o starter Maven:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

As rotas locais esperadas são:

- `GET /swagger-ui.html`: interface navegável do Swagger UI.
- `GET /v3/api-docs`: documento OpenAPI em JSON.

## Configuração global

A configuração global fica em `src/main/java/com/portal/conecta/comunicados/shared/config/OpenApiConfig.java`, pois Swagger/OpenAPI é infraestrutura de documentação da API e não pertence ao domínio de negócio.

Essa configuração define:

- título, descrição e versão da API;
- componentes OpenAPI compartilhados;
- security scheme `bearerAuth` para autenticação JWT.

O esquema `bearerAuth` deve ser definido uma única vez e reaproveitado nas operações autenticadas:

```java
new SecurityScheme()
        .name("bearerAuth")
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
```

Não coloque segredos, chaves privadas, tokens reais, credenciais ou dados pessoais nessa configuração.

## Controllers

Cada controller documentado deve ter `@Tag` com nome e descrição orientados ao contrato da API.

```java
@Tag(name = "Comunicados", description = "Operações para gerenciamento de comunicados.")
@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
}
```

O nome da tag deve representar o recurso HTTP ou a área funcional exposta. A descrição deve ajudar quem consome a API, sem explicar classes internas, use cases ou persistência.

## Endpoints

Cada endpoint deve ter `@Operation` com `summary` curto e `description` quando houver regra de uso relevante para o consumidor.

```java
@Operation(
        summary = "Lista comunicados",
        description = "Retorna comunicados filtrados conforme os parâmetros informados."
)
```

Use `summary` para a ação principal. Use `description` para restrições do contrato, efeitos observáveis, permissões necessárias e comportamento de negócio que afete o consumidor.

Não descreva fluxo interno de aplicação, nomes de métodos privados, repositories, queries ou classes de domínio.

## Parâmetros

Documente path params e query params com `@Parameter`.

```java
@Parameter(description = "Identificador do comunicado.", example = "550e8400-e29b-41d4-a716-446655440000")
@PathVariable UUID announcementId
```

Para query params, informe objetivo, obrigatoriedade e exemplos:

```java
@Parameter(description = "Termo usado para filtrar por título.", example = "reunião")
@RequestParam(required = false) String search
```

Evite exemplos com dados reais. Use valores fictícios, genéricos e seguros.

## Request body

Documente o corpo da requisição com `@io.swagger.v3.oas.annotations.parameters.RequestBody` quando for necessário explicar o payload.

```java
@io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Dados para criação do comunicado.",
        required = true
)
@RequestBody @Valid CreateAnnouncementRequest request
```

O request body deve deixar claro:

- campos obrigatórios;
- formatos esperados;
- restrições de tamanho ou enum;
- exemplos seguros;
- relação com o comportamento externo do endpoint.

Não documente validações como detalhe interno. Documente apenas o que o cliente da API precisa cumprir.

## DTOs e schemas

Use `@Schema` em DTOs de request e response para nome, descrição, exemplos e restrições relevantes.

```java
@Schema(description = "Payload para criação de comunicado.")
public record CreateAnnouncementRequest(
        @Schema(description = "Título do comunicado.", example = "Reunião de alinhamento")
        String title,

        @Schema(description = "Conteúdo do comunicado.")
        String content
) {
}
```

Em enums, descreva o significado externo dos valores quando isso ajudar o consumidor. Não exponha campos internos, flags técnicas, entidades JPA ou estruturas que não fazem parte do contrato HTTP.

## Responses de sucesso

Documente responses de sucesso com `@ApiResponse`, informando status, descrição e schema quando houver corpo.

```java
@ApiResponse(
        responseCode = "201",
        description = "Comunicado criado com sucesso.",
        content = @Content(schema = @Schema(implementation = AnnouncementResponse.class))
)
```

Para endpoints sem corpo, deixe o content vazio:

```java
@ApiResponse(responseCode = "204", description = "Comunicado removido com sucesso.")
```

As descrições devem explicar o resultado observável para o cliente.

## Erros esperados

Documente erros esperados com `@ApiResponse`, usando o schema de erro padrão quando aplicável.

```java
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Requisição inválida.",
                content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
                responseCode = "401",
                description = "Autenticação ausente ou inválida.",
                content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Usuário autenticado sem permissão para executar a operação.",
                content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Recurso não encontrado.",
                content = @Content(schema = @Schema(implementation = ApiError.class))
        )
})
```

Documente somente erros previstos pelo contrato do endpoint. Não inclua stack traces, nomes de exceções internas ou mensagens com dados sensíveis.

## Segurança

Endpoints autenticados devem declarar o uso de `bearerAuth`:

```java
@Operation(
        summary = "Lista comunicados",
        security = @SecurityRequirement(name = "bearerAuth")
)
```

Endpoints públicos, como healthcheck, não devem declarar security requirement.

Nunca inclua token real em exemplos. Quando precisar mostrar o formato, use valores fictícios como `Bearer eyJhbGciOi...`.

## Exemplos

Inclua exemplos quando eles reduzirem ambiguidade para quem consome a API.

```java
@ExampleObject(
        name = "Comunicado criado",
        value = """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "title": "Reunião de alinhamento",
                  "status": "DRAFT"
                }
                """
)
```

Bons exemplos usam dados fictícios, representam casos comuns e respeitam o formato real do contrato. Evite CPF, telefone, e-mail pessoal, tokens, senhas, chaves, identificadores de produção ou qualquer dado sensível.

## O que não colocar no Swagger

Não coloque na documentação Swagger:

- segredo, token real, senha, chave privada ou credencial;
- dados pessoais reais;
- detalhes de banco de dados, tabelas, colunas ou queries;
- entidades JPA como contrato de entrada ou saída;
- ordem interna de chamadas a services, use cases ou repositories;
- regras internas que não afetem o consumidor da API;
- comentários sobre implementação provisória ou débito técnico;
- endpoints que não existam ou formatos de response diferentes do comportamento real.

## Checklist por endpoint

Antes de finalizar a documentação de um endpoint, confira:

- o controller possui `@Tag` clara e orientada ao recurso;
- o endpoint possui `@Operation` com summary objetivo;
- path params e query params possuem `@Parameter` com descrição e exemplo;
- request body está documentado quando existir;
- DTOs de request e response possuem `@Schema` nos campos relevantes;
- responses de sucesso estão documentadas com status correto;
- erros esperados estão documentados com `@ApiResponse`;
- endpoints autenticados declaram `@SecurityRequirement(name = "bearerAuth")`;
- endpoints públicos não declaram autenticação indevida;
- exemplos usam apenas dados fictícios e seguros;
- a documentação descreve o contrato HTTP externo, não a implementação interna;
- nenhum detalhe de banco, entidade JPA, repository ou segredo foi exposto;
- o comportamento documentado foi conferido com o comportamento real do endpoint.
