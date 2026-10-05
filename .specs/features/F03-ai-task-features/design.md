# design.md — F03-ai-task-features

## Visão geral
Porta/adaptador para IA. Prompts em arquivos, structured output + validação pós-LLM, retry.

## Componentes
ai.port (TaskAiPort), ai.adapter (Spring AI), ai.prompt, ai.dto (records tipados).

## DTOs tipados
TaskImprovement{title,description}; TaskAnalysis{priority,complexity(LOW/MEDIUM/HIGH),estimatedHours,double,reason}; TaskDecomposition{subtasks[{title,description,estimatedHours?}]}

## Endpoints
POST /api/ai/tasks/improve; POST /api/ai/tasks/{id}/analyze; POST /api/ai/tasks/{id}/decompose; POST /api/ai/tasks/{id}/decompose/apply.

## Regras
Nunca persiste em improve/decompose; analyze não altera tarefa; aplicação decide; 1 retry controlado → ERR-04; erros de conexão → ERR-03/ERR-05.

## Arquitetura
Domínio/serviço não importam org.springframework.ai. Testes com mocks.
