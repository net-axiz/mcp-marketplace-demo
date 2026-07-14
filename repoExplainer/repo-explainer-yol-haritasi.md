# Repo Açıklayıcı MCP Sunucusu — Yol Haritası

## Hedef

GitHub'da verilen herhangi bir repoyu açıklayabilen bir MCP bileşeni kurmak. Bu bileşen:

- Mevcut `GitMcp` ve `SqlExplainer` MCP sunucularına **client** olarak bağlanır.
- Verilen bir GitHub reposunda README.md varsa içeriğini bulup çeker.
- README yoksa repoyu tarayıp önemli dosyaların içeriğini toplar (fallback modu).
- Toplanan içeriği bir LLM'e (Spring AI `ChatClient` + Anthropic) ileterek repoyu açıklayan bir özet üretir.
- Nihai olarak kendisi de bir MCP Server olarak "Repo Açıklayıcı" aracını dışarıya açabilir.

---

## Adım 1 — Mevcut Sunucuları Ayağa Kaldırmak (Tamamlandı)

- `GitMcp` ve `SqlExplainer` sunucuları SSE/Streamable-HTTP modunda, ayrı portlarda (örn. `:8081`, `:8082`) ayağa kaldırıldı.
- `GitMcp.readDoc` içindeki path traversal koruması (`normalize().toAbsolutePath()` + `startsWith` kontrolü) doğru şekilde mevcut.

### Kod incelemesinden not
- `GitMcp.generateBranchName` metodunda `@McpToolParam` açıklamaları ("Summary of changes." / "Relative issue ID.") parametrelerin (`storyID`, `title`) yeriyle ters düşmüş gibi görünüyor — kontrol edilmeli.
- `storyID` parametre adı camelCase kuralına (`storyId` olmalı) uymuyor; mentorun "naming convention" geri bildirimi bunu da kapsıyor olabilir.

---

## Adım 2 — Orkestratör Modülünü Kurmak

### 2.1 Proje İskeleti
Yeni, bağımsız bir Spring Boot modülü: `repo-explainer-orchestrator` (paket kökü örn. `com.repoexplainer`). Önerilen paket bölünmesi:

- `config` — MCP client bağlantı ayarları, `ChatClient` bean tanımı, GitHub `WebClient` bean'i.
- `github` — GitHub API ile konuşan servis katmanı (README çekme, tree/contents çekme).
- `orchestration` — README var/yok kararını veren, fallback tarama mantığını yürüten, LLM'e gidecek context'i derleyen servis.
- `tool` — orkestratörü kendi MCP Server'ı olarak açan `@McpTool` katmanı (en son eklenecek).
- `dto`/`model` — GitHub API yanıtları için basit veri sınıfları.

### 2.2 Bağımlılıklar
- `spring-ai-starter-mcp-client-webflux` (production için önerilen, reaktif SSE/Streamable-HTTP client)
- Anthropic model starter (LLM çağrısı için `ChatModel`/`ChatClient`)
- `spring-boot-starter-webflux` (hem MCP client webflux hem GitHub API çağrıları için)
- İleride: `spring-ai-starter-mcp-server-webflux` (kendini MCP server olarak açmak için — Adım 6)

Not: `spring-ai-starter-mcp-client-webflux` ile standart `spring-ai-starter-mcp-client` birlikte kullanılmaz. `spring.ai.mcp.client.type` (SYNC/ASYNC) tutarlı seçilmeli, sync/async client karıştırılamaz.

### 2.3 application.yml — MCP Client Bağlantıları
```
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.name=repo-explainer-client
spring.ai.mcp.client.type=SYNC
spring.ai.mcp.client.toolcallback.enabled=true
spring.ai.mcp.client.sse.connections.git-mcp.url=http://localhost:8081
spring.ai.mcp.client.sse.connections.sql-explainer.url=http://localhost:8082
```
K8s ortamında URL'ler Service DNS adlarına döner (örn. `http://gitmcp-service:8081`).

Dikkat: Tool isim çakışması — Spring AI tool'ları isme göre de-duplicate eder (ilk gelen kazanır). Şu an isimler farklı olduğu için sorun yok, yeni araç eklerken kontrol edilmeli.

### 2.4 Bağlantı Doğrulama Checkpoint (Tamamlandı)
Uygulama ayağa kalktığında loglarda "tools discovered" benzeri bir satır görülmesi gerekir. Doğrulama adımları:
1. Sunucuların gerçekten SSE/Streamable-HTTP modunda (stdio değil) çalıştığını kontrol et.
2. Portların erişilebilir olduğunu doğrula.
3. `logging.level.org.springframework.ai.mcp=DEBUG` ile handshake loglarını izle.

### 2.5 ChatClient / Anthropic Bağlantısı
- `spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}` ve model adı tanımlanır.
- `ChatClient.Builder` otomatik enjekte edilir.
- Bu aşamada `.defaultToolCallbacks(...)` bağlanıp bağlanmayacağına (agentic mod mu, deterministik mi) henüz karar verilmez — sadece basit bir promptla `ChatClient`'ın çalıştığı doğrulanır.

---

## ~~Ara Deneme: Gateway/Proxy Yaklaşımı~~ (Kaldırıldı)

`RepoExplainerServer`, `RepoExplainerService`, `RepoExplainerTool` dosyalarında denenen jenerik MCP proxy/gateway pattern'i (tüm alt sunucu araçlarını olduğu gibi yeniden sunma) plana uymadığı ve API kullanımı şüpheli (muhtemelen yanlış paket/tip: `org.springframework.ai.mcp.server.McpServer`, `McpClient` vb.) olduğu için **kaldırıldı**. Sadece Spring Boot giriş sınıfı (`@SpringBootApplication` + `main`) korundu.

---

## Adım 3 — GitHub README Servisi

### 3.1 Paket ve Sorumluluk Ayrımı (`com.repoexplainer.github`)
- **URL parse edici** — GitHub linkini (`https://github.com/owner/repo`, `owner/repo`, `.git` uzantılı, `www.` önekli vb.) `owner`/`repo` çiftine ayıklayan saf fonksiyon.
- **GitHub API istemcisi** — `WebClient` ile `api.github.com`'a istek atan sınıf; `Authorization: Bearer <token>` ve `Accept: application/vnd.github+json` header'ları merkezi tanımlanır. Token ortam değişkeninden okunur.
- **README servisi** — istemciyi kullanan iş mantığı katmanı (var/yok kararı, base64 decode).

### 3.2 Kimlik Doğrulama
- Fine-grained Personal Access Token (`Contents: Read-only` yetkisiyle), ortam değişkeni/K8s Secret olarak.
- Rate limit: tokensız 60/saat, tokenlı 5000/saat.

### 3.3 README Endpoint
`GET https://api.github.com/repos/{owner}/{repo}/readme`
- 200 → `content` alanı (base64) decode edilir.
- 404 → README yok, normal bir sonuç durumu (hata değil) — fallback'e geçilir.
- 403/401 → gerçek hata (rate limit / token sorunu), ayrı ele alınır.

### 3.4 Sonuç Modeli
Üç durumu net ayırt eden bir sealed interface / record yapısı önerilir:
- `Found(String content)`
- `NotFound`
- `Error(String reason)`

`Optional<String>` tek başına yeterli değildir çünkü "yok" (404) ile "hata" (403/network) durumları ayrışmaz.

### 3.5 Checkpoint
Bu servis MCP tool'a/LLM'e bağlanmadan izole test edilmeli:
1. README'si olan bilinen bir repo → `Found` dalı doğrulanır.
2. README'si olmayan bir repo → `NotFound` dalı doğrulanır.
3. Geçersiz token ile → `Error`/401 dalı doğrulanır.

Bu üç senaryo netleşmeden Adım 4'e geçilmemeli.

---

## Adım 4 — Fallback Tarama Mantığı (README Yoksa)

### 4.1 Dosya Ağacını Çekme
`GET /repos/{owner}/{repo}/git/trees/{branch}?recursive=1` — `branch` için önce repo detayından `default_branch` alanı çekilmeli (sabit "main" varsayımı bazı repolarda kırılır).

### 4.2 Önceliklendirme Kuralı
1. Manifest/bağımlılık dosyaları: `pom.xml`, `build.gradle`, `package.json`, `requirements.txt`, `Cargo.toml`, `go.mod`
2. Kök dizindeki diğer `.md` dosyaları
3. `Dockerfile`, `docker-compose.yml`
4. Giriş noktası dosyaları (`Main.java`, `Application.java`, `main.py`, `index.js`, `main.go`)
5. Dizin ağacının kendisi (sadece yollar)

### 4.3 İçerik Çekme ve Budget Yönetimi
- Seçilen dosyalar `GET /repos/{owner}/{repo}/contents/{path}` ile tek tek çekilir (base64).
- Toplam karakter bütçesi (örn. 15-20K) aşılırsa düşük öncelikli dosyalar elenir ya da kısaltılır.
- Bu mantık ayrı, test edilebilir bir fonksiyon olarak tasarlanmalı (girdi: dosya listesi + öncelik skorları, çıktı: budget içinde seçilmiş içerik).

### 4.4 Checkpoint
README'si olmayan bir repo ile fallback akışının doğru dosyaları seçtiği ve toplam boyutun makul kaldığı doğrulanmalı.

---

## Adım 5 — LLM'e İletim ve Prompt Tasarımı

### 5.1 Prompt Şablonu
- **README bulunduğunda:** Projenin ne işe yaradığını, teknolojileri ve kurulumu özetlemesi istenir.
- **Fallback durumunda:** Dosya ağacı ve anahtar dosya içerikleri verilir; LLM'den emin olamadığı noktaları belirtmesi istenir (halüsinasyon riskine karşı).

### 5.2 Deterministik Çağrı (İlk Faz)
İlk fazda `ChatClient`'a tool callback bağlanmadan, derlenen context düz bir `user()` mesajı olarak gönderilir.

### 5.3 Checkpoint
En az 3 repo tipiyle test: iyi README'li repo, README'siz küçük repo, README'siz büyük/karmaşık repo (budget mantığı gerçekten devreye giriyor mu).

---

## Adım 6 — Sonraki Aşamalar (Detaylandırılmadı)

- Agentic tool-calling modeline geçiş (LLM'in kendi kararıyla hangi dosyaya bakacağını seçmesi).
- Orkestratörü kendi MCP Server'ı olarak expose etme (`RepoExplainerTool` içine gerçek `@McpTool` metodu yazılacak).
- Docker Compose ile üç servisin birlikte testi, ardından K8s manifestlerine geçiş.
- Güvenlik notları: GitHub token'ı Secret olarak yönetilmeli; repo boyutu/dosya sayısı limiti konmalı; README/dosya içeriği kullanıcıdan gelen veri sayıldığından prompt injection riskine dikkat edilmeli.
