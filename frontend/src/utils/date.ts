export function formatarDataBR(iso: string | null): string {
  if (!iso) {
    return '';
  }
  return iso.slice(0, 10).split('-').reverse().join('/');
}

export function dataHojeLocal(): string {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
}