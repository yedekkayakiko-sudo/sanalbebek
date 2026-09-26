import { readFileSync, readdirSync, existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

export interface Skill {
  name: string;
  description: string;
  events: string[];
  body: string;
}

const here = path.dirname(fileURLToPath(import.meta.url));
export const DEFAULT_SKILLS_DIR = path.resolve(here, "../../skills");

// Basit frontmatter ayrıştırıcı: name, description ve events: [a, b] satırlarını okur.
export function parseSkill(text: string): Skill {
  const m = text.match(/^---\n([\s\S]*?)\n---\n?([\s\S]*)$/);
  if (!m) throw new Error("SKILL.md frontmatter eksik");
  const meta: Record<string, string> = {};
  for (const line of m[1].split("\n")) {
    const i = line.indexOf(":");
    if (i > 0) meta[line.slice(0, i).trim()] = line.slice(i + 1).trim();
  }
  if (!meta.name || !meta.description) throw new Error("SKILL.md name/description eksik");
  const events = (meta.events ?? "").replace(/^\[|\]$/g, "").split(",").map(s => s.trim()).filter(Boolean);
  return { name: meta.name, description: meta.description, events, body: m[2].trim() };
}

export function loadSkills(dir = DEFAULT_SKILLS_DIR): Skill[] {
  if (!existsSync(dir)) throw new Error(`Skill klasörü yok: ${dir}`);
  return readdirSync(dir, { withFileTypes: true })
    .filter(d => d.isDirectory() && existsSync(path.join(dir, d.name, "SKILL.md")))
    .map(d => parseSkill(readFileSync(path.join(dir, d.name, "SKILL.md"), "utf8")))
    .sort((a, b) => a.name.localeCompare(b.name));   // sabit sıra: prompt önbelleği bozulmasın
}

// Sistem istemi: tüm skill'ler, sabit sırayla. Olaya göre filtrelemek yerine hepsini
// yüklemek önek önbelleğini (prompt caching) her olayda aynı tutar ve daha ucuzdur.
export function buildSystemPrompt(skills: Skill[]): string {
  const index = skills.map(s => `- ${s.name}: ${s.description} (olaylar: ${s.events.join(", ")})`).join("\n");
  const bodies = skills.map(s => `<skill name="${s.name}">\n${s.body}\n</skill>`).join("\n\n");
  return [
    "Aşağıdaki skill'ler bir duvar kağıdı karakterinin beynini tanımlar.",
    "Her istekte gelen `olay` için, olaylar listesinde o olayı ya da `her-zaman` içeren skill'lerin kurallarını uygula.",
    "",
    "Skill listesi:",
    index,
    "",
    bodies,
  ].join("\n");
}
