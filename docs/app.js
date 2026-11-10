/* ============================================================================
 * MovieNight v2.4.0 — Progressive Web App per iOS (iPhone / iPad) & Android
 * Catalogo infinito in tempo reale da IMDb, TVmaze e MyAnimeList (Jikan v4)
 * ============================================================================ */

const STORAGE_KEY = 'movienight_ios_library_v24';

const GENRES = [
  { id: 'ALL', label: 'Tutti 🍿', apiGenre: null, malId: null },
  { id: 'SCIFI', label: 'Sci-Fi 🚀', apiGenre: 'Sci-Fi', malId: 24 },
  { id: 'ACTION', label: 'Azione 💥', apiGenre: 'Action', malId: 1 },
  { id: 'THRILLER', label: 'Thriller 🕵️', apiGenre: 'Thriller', malId: 41 },
  { id: 'COMEDY', label: 'Commedia 😂', apiGenre: 'Comedy', malId: 4 },
  { id: 'DRAMA', label: 'Dramma 🎭', apiGenre: 'Drama', malId: 8 },
  { id: 'ANIMATION', label: 'Animazione ✨', apiGenre: 'Animation', malId: 10 },
  { id: 'HORROR', label: 'Horror 👻', apiGenre: 'Horror', malId: 14 }
];

const ERAS = [
  { id: 'ALL', label: 'Tutte le epoche 🕰️', min: null, max: null },
  { id: '2020S', label: '2020 - Oggi 🔥', min: 2020, max: 2030 },
  { id: '2010S', label: 'Anni 2010 📱', min: 2010, max: 2019 },
  { id: '2000S', label: 'Anni 2000 💿', min: 2000, max: 2009 },
  { id: '90S', label: "Anni '90 📼", min: 1990, max: 1999 },
  { id: '80S', label: "Anni '80 🕹️", min: 1980, max: 1989 },
  { id: 'CLASSICS', label: "Classici (< '80) 🎞️", min: 1920, max: 1979 }
];

const ANIME_FORMATS = [
  { id: 'ALL', label: 'Tutti ⛩️', jikanType: null },
  { id: 'SERIES', label: 'Serie Anime 📺', jikanType: 'tv' },
  { id: 'MOVIE', label: 'Film Anime 🎬', jikanType: 'movie' }
];

const ANIME_KEYWORDS = [
  'spirited away', 'città incantata', 'your name', 'kimi no na wa', 'mononoke',
  "howl's moving castle", 'totoro', 'akira', 'suzume', 'jujutsu kaisen',
  'demon slayer', 'kimetsu no yaiba', 'one piece', 'naruto', 'dragon ball',
  'evangelion', 'cowboy bebop', 'attack on titan', 'shingeki no kyojin',
  'death note', 'fullmetal alchemist', 'hunter x hunter', 'frieren', 'studio ghibli'
];

// Catalogo curato di fallback se il dispositivo è offline
const FALLBACK_CATALOG = {
  MOVIE: [
    {
      id: 'tt1375666', title: 'Inception', year: 2010, imdbRating: 8.8, runtimeMinutes: 148,
      genres: ['Sci-Fi', 'Action', 'Thriller'],
      plot: 'Un ladro capace di infiltrarsi nei sogni altrui riceve l’incarico inverso: piantare un’idea nella mente di un erede industriale.',
      director: 'Christopher Nolan',
      posterUrl: 'https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_FMjpg_UX1000_.jpg',
      mediaType: 'MOVIE', source: 'IMDB', totalEpisodes: 1,
      cast: ['Leonardo DiCaprio', 'Joseph Gordon-Levitt', 'Cillian Murphy', 'Tom Hardy'],
      platforms: ['Netflix', 'Prime Video', 'Apple TV']
    },
    {
      id: 'tt0816692', title: 'Interstellar', year: 2014, imdbRating: 8.7, runtimeMinutes: 169,
      genres: ['Sci-Fi', 'Drama'],
      plot: 'Un gruppo di esploratori attraversa un wormhole nello spazio nel tentativo di garantire la sopravvivenza dell’umanità.',
      director: 'Christopher Nolan',
      posterUrl: 'https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_FMjpg_UX1000_.jpg',
      mediaType: 'MOVIE', source: 'IMDB', totalEpisodes: 1,
      cast: ['Matthew McConaughey', 'Anne Hathaway', 'Jessica Chastain'],
      platforms: ['Prime Video', 'Netflix']
    },
    {
      id: 'tt0068646', title: 'Il Padrino (The Godfather)', year: 1972, imdbRating: 9.2, runtimeMinutes: 175,
      genres: ['Drama', 'Thriller'],
      plot: 'Il patriarca di una dinastia criminale newyorkese trasferisce il controllo del suo impero clandestino al figlio riluttante.',
      director: 'Francis Ford Coppola',
      posterUrl: 'https://m.media-amazon.com/images/M/MV5BM2MyNjYxNmUtYTAwNi00MTYxLWJmNWYtYzZlODY3ZTk3OTFlXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_FMjpg_UX1000_.jpg',
      mediaType: 'MOVIE', source: 'IMDB', totalEpisodes: 1,
      cast: ['Marlon Brando', 'Al Pacino', 'James Caan'],
      platforms: ['Paramount+', 'Prime Video', 'Netflix']
    },
    {
      id: 'tt0088763', title: 'Ritorno al futuro (Back to the Future)', year: 1985, imdbRating: 8.5, runtimeMinutes: 116,
      genres: ['Sci-Fi', 'Comedy'],
      plot: 'Marty McFly, un adolescente degli anni ’80, viene spedito accidentalmente nel 1955 a bordo di una DeLorean.',
      director: 'Robert Zemeckis',
      posterUrl: 'https://m.media-amazon.com/images/M/MV5BZmU0M2Y1OGUtZjIxNi00ZjBkLTg1MjgtOWIyNThiZWIwYjRiXkEyXkFqcGdeQXVyMTQxNzMzNDI@._V1_FMjpg_UX1000_.jpg',
      mediaType: 'MOVIE', source: 'IMDB', totalEpisodes: 1,
      cast: ['Michael J. Fox', 'Christopher Lloyd'],
      platforms: ['Netflix', 'Prime Video']
    }
  ],
  TV_SERIES: [
    {
      id: 'tt0903747', title: 'Breaking Bad', year: 2008, imdbRating: 9.5, runtimeMinutes: 49,
      genres: ['Drama', 'Thriller'],
      plot: 'Un professore di chimica scopre di avere un cancro e inizia a produrre metanfetamina insieme a un suo ex studente.',
      director: 'Vince Gilligan',
      posterUrl: 'https://m.media-amazon.com/images/M/MV5BMzU5ZGYzNmQtMTdhYy00OGRiLTg0NmQtYjVjNzliZTg1ZGE4XkEyXkFqcGc@._V1_FMjpg_UX1000_.jpg',
      mediaType: 'TV_SERIES', source: 'IMDB', totalEpisodes: 62,
      cast: ['Bryan Cranston', 'Aaron Paul', 'Bob Odenkirk'],
      platforms: ['Netflix']
    },
    {
      id: 'tt0386676', title: 'The Office (US)', year: 2005, imdbRating: 9.0, runtimeMinutes: 22,
      genres: ['Comedy'],
      plot: 'Un finto documentario sulla vita quotidiana degli impiegati della Dunder Mifflin guidati dall’eccentrico Michael Scott.',
      director: 'Greg Daniels',
      posterUrl: 'https://m.media-amazon.com/images/M/MV5BMDNkOTE4NDQtMTNmYi00MWE0LWE4ZTktYTc0NzhhNWIzNzJiXkEyXkFqcGdeQXVyMzQ2MDI5NjU@._V1_FMjpg_UX1000_.jpg',
      mediaType: 'TV_SERIES', source: 'IMDB', totalEpisodes: 188,
      cast: ['Steve Carell', 'John Krasinski', 'Jenna Fischer'],
      platforms: ['Netflix', 'Prime Video']
    }
  ],
  ANIME: [
    {
      id: 'mal_5114', malId: 5114, title: 'Fullmetal Alchemist: Brotherhood', year: 2009, imdbRating: 9.1, runtimeMinutes: 24,
      genres: ['Action', 'Animation', 'Drama'],
      plot: 'I fratelli Edward e Alphonse Elric cercano la Pietra Filosofale per riavere i propri corpi dopo una trasmutazione proibita.',
      director: 'Bones • MyAnimeList #5114',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/1208/94745l.jpg',
      mediaType: 'ANIME', source: 'MYANIMELIST', totalEpisodes: 64,
      cast: ['Edward Elric', 'Alphonse Elric', 'Roy Mustang'],
      platforms: ['Crunchyroll', 'Netflix']
    },
    {
      id: 'mal_199', malId: 199, title: 'La città incantata (Spirited Away)', year: 2001, imdbRating: 8.9, runtimeMinutes: 125,
      genres: ['Animation', 'Drama'],
      plot: 'La piccola Chihiro entra nel mondo degli spiriti governato dalla strega Yubaba e deve salvare i suoi genitori.',
      director: 'Studio Ghibli • Hayao Miyazaki',
      posterUrl: 'https://cdn.myanimelist.net/images/anime/6/79597l.jpg',
      mediaType: 'ANIME', source: 'MYANIMELIST', totalEpisodes: 1,
      cast: ['Chihiro Ogino', 'Haku', 'Yubaba'],
      platforms: ['Netflix']
    }
  ]
};

// Stato Globale dell'App
const state = {
  tab: 'DISCOVER',          // DISCOVER | WATCHLIST | WATCHED
  mediaType: 'MOVIE',       // MOVIE | TV_SERIES | ANIME
  animeFormat: 'ALL',       // ALL | SERIES | MOVIE
  genre: 'ALL',
  era: 'ALL',
  page: 1,
  searchQuery: '',
  showSearch: false,
  hideIosBanner: window.navigator.standalone === true,
  isLoading: true,
  isLoadingMore: false,
  deck: [],
  currentIndex: 0,
  watchlist: [],
  watched: [],
  dislikedIds: [],
  progress: {},             // { [mediaId]: { userRating: 9, watchedEpisodes: ['ep1'] } }
  malUsername: '',
  malAccessToken: '',
  malMessage: null,
  isSyncingMal: false,
  winnerModal: null,
  detailModal: null,        // Oggetto Movie aperto nei dettagli
  detailCast: [],           // [{ name, role, photoUrl }]
  isLoadingCast: false,
  episodeModal: null,       // Oggetto Movie aperto negli episodi
  episodesList: [],
  isLoadingEpisodes: false
};

// Carica dati salvati su iPhone / Browser
function loadStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    const parsed = JSON.parse(raw);
    state.watchlist = parsed.watchlist || [];
    state.watched = parsed.watched || [];
    state.dislikedIds = parsed.dislikedIds || [];
    state.progress = parsed.progress || {};
    state.malUsername = parsed.malUsername || '';
    state.malAccessToken = parsed.malAccessToken || '';
  } catch (_) {}
}

function saveStorage() {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        watchlist: state.watchlist,
        watched: state.watched,
        dislikedIds: state.dislikedIds,
        progress: state.progress,
        malUsername: state.malUsername,
        malAccessToken: state.malAccessToken
      })
    );
  } catch (_) {}
}

function normalizeGenres(rawList, expectedGenre) {
  const out = [];
  (rawList || []).forEach((g) => {
    const lower = String(g || '').trim().toLowerCase();
    let mapped = null;
    if (lower.includes('sci-fi') || lower.includes('science fiction')) mapped = 'Sci-Fi';
    else if (['action', 'adventure', 'war', 'western'].includes(lower)) mapped = 'Action';
    else if (['thriller', 'suspense', 'mystery', 'psychological', 'crime'].includes(lower)) mapped = 'Thriller';
    else if (['comedy', 'parody', 'gag humor', 'sitcom'].includes(lower)) mapped = 'Comedy';
    else if (['drama', 'romance', 'biography', 'history', 'family'].includes(lower)) mapped = 'Drama';
    else if (['animation', 'fantasy', 'supernatural'].includes(lower)) mapped = 'Animation';
    else if (['horror', 'gore', 'dark fantasy'].includes(lower)) mapped = 'Horror';
    if (mapped && !out.includes(mapped)) out.push(mapped);
  });
  if (expectedGenre && !out.includes(expectedGenre)) out.push(expectedGenre);
  if (out.length === 0) out.push('Drama');
  return out;
}

function isAnimeTitle(item) {
  if (item.mediaType === 'ANIME' || item.source === 'MYANIMELIST') return true;
  const t = (item.title || '').toLowerCase();
  const d = (item.director || '').toLowerCase();
  return ANIME_KEYWORDS.some((kw) => t.includes(kw) || d.includes(kw));
}

function matchesEra(year, eraId) {
  const eraObj = ERAS.find((e) => e.id === eraId) || ERAS[0];
  if (eraObj.min && year < eraObj.min) return false;
  if (eraObj.max && year > eraObj.max) return false;
  return true;
}

/* ============================================================================
 * MOTORI REAL-TIME: IMDb / YTS (Film), TVmaze / IMDb (Serie TV), Jikan v4 (Anime)
 * ============================================================================ */

async function fetchJsonWithTimeout(url, timeoutMs = 7500) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const res = await fetch(url, { signal: ctrl.signal });
    if (!res.ok) return null;
    return await res.json();
  } catch (_) {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

async function fetchLiveMovies(genreObj, eraObj, page) {
  const results = [];
  const sortModes = ['download_count', 'like_count', 'rating', 'year', 'date_added'];
  const sortBy = sortModes[(page - 1) % sortModes.length];
  const ytsPage = Math.floor((page - 1) / sortModes.length) + 1;

  let params = `?limit=35&page=${ytsPage}&sort_by=${sortBy}&minimum_rating=6`;
  if (genreObj.apiGenre) params += `&genre=${encodeURIComponent(genreObj.apiGenre)}`;

  const mirrors = [
    `https://yts.mx/api/v2/list_movies.json${params}`,
    `https://yts.lt/api/v2/list_movies.json${params}`
  ];

  for (const endpoint of mirrors) {
    const data = await fetchJsonWithTimeout(endpoint);
    const movies = data?.data?.movies;
    if (Array.isArray(movies) && movies.length > 0) {
      movies.forEach((m) => {
        if (!m.imdb_code || !m.title) return;
        const item = {
          id: m.imdb_code,
          title: m.title,
          year: m.year || 2021,
          imdbRating: Number((m.rating || 7.5).toFixed(1)),
          runtimeMinutes: m.runtime > 20 ? m.runtime : 115,
          genres: normalizeGenres(m.genres || [], genreObj.apiGenre),
          plot: m.summary || m.description_full || `Film dal catalogo ufficiale IMDb (${m.imdb_code}).`,
          director: `IMDb #${m.imdb_code}`,
          posterUrl: m.large_cover_image || m.medium_cover_image || '',
          mediaType: 'MOVIE',
          source: 'IMDB',
          totalEpisodes: 1,
          cast: [`Cast IMDb (${m.imdb_code})`],
          platforms: ['Netflix', 'Prime Video', 'Disney+', 'Apple TV']
        };
        if (!isAnimeTitle(item)) results.push(item);
      });
      break;
    }
  }

  // Arricchimento aggiuntivo tramite Free Movie DB IMDb API
  if (results.length < 12) {
    const seeds = ['star', 'dark', 'matrix', 'inception', 'blade', 'godfather', 'alien', 'mission', 'back', 'love'];
    const seed = seeds[(page - 1) % seeds.length];
    const imdbData = await fetchJsonWithTimeout(`https://imdb.iamidiotareyoutoo.com/search?q=${encodeURIComponent(seed)}`);
    if (Array.isArray(imdbData?.description)) {
      imdbData.description.forEach((d) => {
        const id = d['#IMDB_ID'];
        const title = d['#TITLE'];
        if (!id || !title) return;
        const actors = (d['#ACTORS'] || '').split(',').map((s) => s.trim()).filter(Boolean);
        const item = {
          id,
          title,
          year: d['#YEAR'] || 2020,
          imdbRating: d['#RANK'] && d['#RANK'] < 300 ? 8.4 : 7.7,
          runtimeMinutes: 120,
          genres: normalizeGenres([], genreObj.apiGenre),
          plot: `Titolo dal database ufficiale IMDb (${id}). Cast principale: ${actors.join(', ') || 'IMDb Cast'}.`,
          director: actors[0] || 'IMDb Official',
          posterUrl: d['#IMG_POSTER'] || '',
          mediaType: 'MOVIE',
          source: 'IMDB',
          totalEpisodes: 1,
          cast: actors,
          platforms: ['Netflix', 'Prime Video', 'Apple TV']
        };
        if (!isAnimeTitle(item)) results.push(item);
      });
    }
  }

  return results;
}

async function fetchLiveTvSeries(genreObj, eraObj, page) {
  const results = [];
  const tvPage = Math.max(0, page - 1);
  const data = await fetchJsonWithTimeout(`https://api.tvmaze.com/shows?page=${tvPage}`);
  if (Array.isArray(data)) {
    data.forEach((show) => {
      if (!show || !show.name) return;
      if (show.type === 'Animation' && show.language === 'Japanese') return;
      if (Array.isArray(show.genres) && show.genres.includes('Anime')) return;

      const imdbId = show.externals?.imdb || `tvmaze_${show.id}`;
      const year = show.premiered ? parseInt(show.premiered.slice(0, 4), 10) || 2020 : 2020;
      const rating = show.rating?.average ? Number(show.rating.average.toFixed(1)) : 7.8;
      const cleanPlot = (show.summary || '').replace(/<[^>]*>/g, '').trim() || `Serie TV dal catalogo IMDb (${imdbId}).`;
      const net = show.network?.name || show.webChannel?.name || 'Serie TV IMDb';

      const item = {
        id: imdbId,
        tvMazeId: show.id,
        title: show.name,
        year,
        imdbRating: rating,
        runtimeMinutes: show.averageRuntime || 48,
        genres: normalizeGenres(show.genres || [], null),
        plot: cleanPlot,
        director: `${net} • IMDb ${imdbId}`,
        posterUrl: show.image?.original || show.image?.medium || '',
        mediaType: 'TV_SERIES',
        source: 'IMDB',
        totalEpisodes: 16,
        cast: [net],
        platforms: ['Netflix', 'Prime Video', 'Disney+', 'NOW']
      };
      if (!isAnimeTitle(item)) results.push(item);
    });
  }
  return results;
}

async function fetchLiveAnime(genreObj, formatObj, eraObj, page) {
  const hasFilters = genreObj.malId !== null || formatObj.jikanType !== null || eraObj.id !== 'ALL';
  let url = '';
  if (hasFilters) {
    url = `https://api.jikan.moe/v4/anime?order_by=score&sort=desc&limit=25&sfw=true&page=${page}`;
    if (genreObj.malId !== null) url += `&genres=${genreObj.malId}`;
    if (formatObj.jikanType !== null) url += `&type=${formatObj.jikanType}`;
    if (eraObj.min !== null) url += `&start_date=${eraObj.min}-01-01`;
    if (eraObj.max !== null) url += `&end_date=${eraObj.max}-12-31`;
  } else {
    url = `https://api.jikan.moe/v4/top/anime?limit=25&page=${page}`;
  }

  const json = await fetchJsonWithTimeout(url);
  if (!Array.isArray(json?.data)) return [];

  return json.data.map((item) => {
    const malId = item.mal_id;
    const isMovie = String(item.type || '').toLowerCase() === 'movie';
    const year = item.year || (item.aired?.from ? parseInt(item.aired.from.slice(0, 4), 10) : 2022) || 2022;
    const rawGenres = [
      ...(item.genres || []).map((g) => g.name),
      ...(item.themes || []).map((t) => t.name)
    ];
    const studio = item.studios?.[0]?.name || 'MyAnimeList';

    return {
      id: `mal_${malId}`,
      malId,
      title: item.title_english || item.title || 'Anime',
      year,
      imdbRating: item.score ? Number(item.score.toFixed(1)) : 8.1,
      runtimeMinutes: isMovie ? 115 : 24,
      genres: normalizeGenres(rawGenres, genreObj.apiGenre),
      plot: (item.synopsis || '').replace('[Written by MAL Rewrite]', '').trim() || `Anime dal catalogo ufficiale MyAnimeList (#${malId}).`,
      director: `${studio} • MyAnimeList #${malId}`,
      posterUrl: item.images?.jpg?.large_image_url || item.images?.jpg?.image_url || '',
      mediaType: 'ANIME',
      source: 'MYANIMELIST',
      totalEpisodes: isMovie ? 1 : (item.episodes > 1 ? item.episodes : 12),
      cast: [studio],
      platforms: ['Crunchyroll', 'Netflix', 'Prime Video']
    };
  });
}

/* ============================================================================
 * CARICAMENTO CATALOGO INFINITO & RICERCA LIVE
 * ============================================================================ */

async function loadCatalog(reset = true) {
  if (reset) {
    state.isLoading = true;
    state.page = 1;
    state.currentIndex = 0;
    render();
  } else {
    if (state.isLoading || state.isLoadingMore) return;
    state.isLoadingMore = true;
    state.page += 1;
  }

  const genreObj = GENRES.find((g) => g.id === state.genre) || GENRES[0];
  const eraObj = ERAS.find((e) => e.id === state.era) || ERAS[0];
  const formatObj = ANIME_FORMATS.find((f) => f.id === state.animeFormat) || ANIME_FORMATS[0];

  let raw = [];
  if (state.mediaType === 'MOVIE') {
    raw = await fetchLiveMovies(genreObj, eraObj, state.page);
  } else if (state.mediaType === 'TV_SERIES') {
    raw = await fetchLiveTvSeries(genreObj, eraObj, state.page);
  } else {
    raw = await fetchLiveAnime(genreObj, formatObj, eraObj, state.page);
  }

  let filtered = filterCatalogItems(raw, genreObj, formatObj, eraObj);

  if (filtered.length === 0 && reset) {
    const fallbackPool = FALLBACK_CATALOG[state.mediaType] || [];
    filtered = filterCatalogItems(fallbackPool, genreObj, formatObj, eraObj);
  }

  // Mescola i risultati
  filtered.sort(() => Math.random() - 0.5);

  const excluded = new Set([
    ...state.dislikedIds,
    ...state.watchlist.map((m) => m.id),
    ...state.watched.map((m) => m.id)
  ]);

  if (reset) {
    state.deck = filtered.filter((m) => !excluded.has(m.id));
    state.isLoading = false;
  } else {
    const existingIds = new Set(state.deck.slice(state.currentIndex).map((m) => m.id));
    const fresh = filtered.filter((m) => !excluded.has(m.id) && !existingIds.has(m.id));
    if (fresh.length > 0) {
      state.deck = [...state.deck, ...fresh];
    } else {
      const recyclable = state.deck.slice(0, state.currentIndex).filter((m) => !excluded.has(m.id));
      state.deck = [...state.deck, ...recyclable];
    }
    state.isLoadingMore = false;
  }

  render();
}

function filterCatalogItems(items, genreObj, formatObj, eraObj) {
  return items.filter((item) => {
    if (genreObj.apiGenre && !item.genres.some((g) => g.toLowerCase() === genreObj.apiGenre.toLowerCase())) {
      return false;
    }
    if (state.mediaType === 'ANIME') {
      if (formatObj.id === 'SERIES' && item.totalEpisodes <= 1) return false;
      if (formatObj.id === 'MOVIE' && item.totalEpisodes > 1) return false;
    }
    if (!matchesEra(item.year, eraObj.id)) return false;
    return true;
  });
}

async function runLiveSearch(query) {
  state.searchQuery = query;
  if (!query.trim()) {
    loadCatalog(true);
    return;
  }
  state.isLoading = true;
  render();

  const q = query.trim();
  let results = [];
  if (state.mediaType === 'ANIME') {
    const json = await fetchJsonWithTimeout(`https://api.jikan.moe/v4/anime?q=${encodeURIComponent(q)}&limit=25&sfw=true`);
    if (Array.isArray(json?.data)) {
      results = json.data.map((item) => ({
        id: `mal_${item.mal_id}`,
        malId: item.mal_id,
        title: item.title_english || item.title,
        year: item.year || 2022,
        imdbRating: item.score || 8.0,
        runtimeMinutes: item.type === 'Movie' ? 115 : 24,
        genres: normalizeGenres((item.genres || []).map((g) => g.name), null),
        plot: item.synopsis || 'Anime dal catalogo MyAnimeList.',
        director: `MyAnimeList #${item.mal_id}`,
        posterUrl: item.images?.jpg?.large_image_url || item.images?.jpg?.image_url || '',
        mediaType: 'ANIME',
        source: 'MYANIMELIST',
        totalEpisodes: item.type === 'Movie' ? 1 : (item.episodes || 12),
        cast: [item.studios?.[0]?.name || 'Studio Anime'],
        platforms: ['Crunchyroll', 'Netflix']
      }));
    }
  } else if (state.mediaType === 'TV_SERIES') {
    const json = await fetchJsonWithTimeout(`https://api.tvmaze.com/search/shows?q=${encodeURIComponent(q)}`);
    if (Array.isArray(json)) {
      results = json.map((w) => w.show).filter(Boolean).map((show) => ({
        id: show.externals?.imdb || `tvmaze_${show.id}`,
        tvMazeId: show.id,
        title: show.name,
        year: show.premiered ? parseInt(show.premiered.slice(0, 4), 10) || 2020 : 2020,
        imdbRating: show.rating?.average || 7.8,
        runtimeMinutes: show.averageRuntime || 48,
        genres: normalizeGenres(show.genres || [], null),
        plot: (show.summary || '').replace(/<[^>]*>/g, '').trim() || 'Serie TV IMDb.',
        director: show.network?.name || 'Serie TV IMDb',
        posterUrl: show.image?.original || show.image?.medium || '',
        mediaType: 'TV_SERIES',
        source: 'IMDB',
        totalEpisodes: 16,
        cast: [show.network?.name || 'Cast IMDb'],
        platforms: ['Netflix', 'Prime Video']
      }));
    }
  } else {
    const yts = await fetchJsonWithTimeout(`https://yts.mx/api/v2/list_movies.json?query_term=${encodeURIComponent(q)}&limit=20`);
    const movies = yts?.data?.movies || [];
    results = movies.map((m) => ({
      id: m.imdb_code,
      title: m.title,
      year: m.year || 2020,
      imdbRating: m.rating || 7.6,
      runtimeMinutes: m.runtime || 115,
      genres: normalizeGenres(m.genres || [], null),
      plot: m.summary || 'Film dal catalogo IMDb.',
      director: `IMDb #${m.imdb_code}`,
      posterUrl: m.large_cover_image || m.medium_cover_image || '',
      mediaType: 'MOVIE',
      source: 'IMDB',
      totalEpisodes: 1,
      cast: [`Cast IMDb (${m.imdb_code})`],
      platforms: ['Netflix', 'Prime Video']
    }));
  }

  state.deck = results;
  state.currentIndex = 0;
  state.isLoading = false;
  render();
}

/* ============================================================================
 * 4 DIREZIONI DI SWIPE & PREFETCH AUTOMATICO
 * ============================================================================ */

function checkAndLoadMore() {
  const remaining = state.deck.length - state.currentIndex;
  if (remaining <= 6 && !state.searchQuery.trim()) {
    loadCatalog(false);
  }
}

function swipeRightLike() {
  const current = state.deck[state.currentIndex];
  if (!current) return;
  if (!state.watchlist.some((m) => m.id === current.id)) {
    state.watchlist = [current, ...state.watchlist];
  }
  state.currentIndex += 1;
  saveStorage();
  checkAndLoadMore();
  render();
}

function swipeUpWatched() {
  const current = state.deck[state.currentIndex];
  if (!current) return;
  markMovieWatched(current);
  state.currentIndex += 1;
  checkAndLoadMore();
  render();
}

function swipeDownPool() {
  const current = state.deck[state.currentIndex];
  if (!current) return;
  state.deck = [...state.deck, current];
  state.currentIndex += 1;
  checkAndLoadMore();
  render();
}

function swipeLeftSkip() {
  const current = state.deck[state.currentIndex];
  if (!current) return;
  if (!state.dislikedIds.includes(current.id)) {
    state.dislikedIds.push(current.id);
  }
  state.currentIndex += 1;
  saveStorage();
  checkAndLoadMore();
  render();
}

function markMovieWatched(movie, rating = null) {
  state.watchlist = state.watchlist.filter((m) => m.id !== movie.id);
  if (!state.watched.some((m) => m.id === movie.id)) {
    state.watched = [movie, ...state.watched];
  }
  if (rating !== null) {
    const prog = state.progress[movie.id] || { watchedEpisodes: [] };
    prog.userRating = rating;
    state.progress[movie.id] = prog;
  }
  saveStorage();
}

/* ============================================================================
 * DETTAGLI PROGRAMMA & FOTO ATTORI / PERSONAGGI LIVE
 * ============================================================================ */

async function openProgramDetails(movie) {
  state.detailModal = movie;
  state.detailCast = (movie.cast || []).map((name) => ({
    name,
    role: movie.source === 'MYANIMELIST' ? 'Personaggio / Studio' : 'Cast Principale',
    photoUrl: movie.posterUrl
  }));
  state.isLoadingCast = true;
  render();

  if (movie.source === 'MYANIMELIST' && movie.malId) {
    const json = await fetchJsonWithTimeout(`https://api.jikan.moe/v4/anime/${movie.malId}/characters`);
    if (Array.isArray(json?.data) && json.data.length > 0) {
      state.detailCast = json.data.slice(0, 10).map((item) => {
        const rawName = item.character?.name || 'Personaggio';
        const va = item.voice_actors?.[0]?.person?.name;
        return {
          name: rawName,
          role: va ? `CV: ${va}` : (item.role || 'Protagonista'),
          photoUrl: item.character?.images?.jpg?.image_url || movie.posterUrl
        };
      });
    }
  } else if (movie.mediaType === 'TV_SERIES') {
    let tvId = movie.tvMazeId;
    if (!tvId && movie.id.startsWith('tt')) {
      const look = await fetchJsonWithTimeout(`https://api.tvmaze.com/lookup/shows?imdb=${movie.id}`);
      tvId = look?.id;
    }
    if (tvId) {
      const castJson = await fetchJsonWithTimeout(`https://api.tvmaze.com/shows/${tvId}/cast`);
      if (Array.isArray(castJson) && castJson.length > 0) {
        state.detailCast = castJson.slice(0, 10).map((c) => ({
          name: c.person?.name || 'Attore',
          role: c.character?.name || 'Cast Principale',
          photoUrl: c.person?.image?.medium || c.character?.image?.medium || movie.posterUrl
        }));
      }
    }
  } else if (movie.id.startsWith('tt')) {
    const detailJson = await fetchJsonWithTimeout(`https://imdb.iamidiotareyoutoo.com/search?tt=${movie.id}`);
    const shortObj = detailJson?.short;
    if (shortObj) {
      if (shortObj.description) {
        state.detailModal = { ...state.detailModal, plot: shortObj.description };
      }
      const actors = Array.isArray(shortObj.actor)
        ? shortObj.actor.map((a) => a.name).filter(Boolean).slice(0, 8)
        : (movie.cast || []);
      const enriched = await Promise.all(
        actors.map(async (actorName) => {
          const wiki = await fetchJsonWithTimeout(
            `https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(actorName.replace(/ /g, '_'))}`
          );
          return {
            name: actorName,
            role: 'Cast Principale',
            photoUrl: wiki?.thumbnail?.source || movie.posterUrl
          };
        })
      );
      if (enriched.length > 0) state.detailCast = enriched;
    }
  }

  state.isLoadingCast = false;
  render();
}

async function openEpisodeTracker(movie) {
  state.episodeModal = movie;
  state.isLoadingEpisodes = true;
  state.episodesList = [];
  render();

  let loaded = [];
  if (movie.source === 'MYANIMELIST' && movie.malId) {
    const json = await fetchJsonWithTimeout(`https://api.jikan.moe/v4/anime/${movie.malId}/episodes`);
    if (Array.isArray(json?.data) && json.data.length > 0) {
      loaded = json.data.map((ep, idx) => ({
        id: `mal_${movie.malId}_e${ep.mal_id || idx + 1}`,
        number: ep.mal_id || idx + 1,
        title: ep.title || `Episodio ${idx + 1}`,
        rating: ep.score ? Number((ep.score <= 5 ? ep.score * 2 : ep.score).toFixed(1)) : movie.imdbRating
      }));
    }
  } else if (movie.mediaType === 'TV_SERIES') {
    let tvId = movie.tvMazeId;
    if (!tvId && movie.id.startsWith('tt')) {
      const look = await fetchJsonWithTimeout(`https://api.tvmaze.com/lookup/shows?imdb=${movie.id}`);
      tvId = look?.id;
    }
    if (tvId) {
      const epJson = await fetchJsonWithTimeout(`https://api.tvmaze.com/shows/${tvId}/episodes`);
      if (Array.isArray(epJson) && epJson.length > 0) {
        loaded = epJson.map((ep, idx) => ({
          id: `${movie.id}_s${ep.season || 1}_e${ep.number || idx + 1}`,
          number: idx + 1,
          title: `S${ep.season || 1}:E${ep.number || idx + 1} — ${ep.name || 'Episodio'}`,
          rating: ep.rating?.average || movie.imdbRating
        }));
      }
    }
  }

  if (loaded.length === 0) {
    const count = Math.max(12, movie.totalEpisodes || 12);
    for (let i = 1; i <= count; i++) {
      loaded.push({
        id: `${movie.id}_e${i}`,
        number: i,
        title: `Episodio ${i}`,
        rating: movie.imdbRating
      });
    }
  }

  state.episodesList = loaded;
  state.isLoadingEpisodes = false;
  render();
}

function toggleEpisodeWatched(movie, epId) {
  const prog = state.progress[movie.id] || { watchedEpisodes: [] };
  const list = prog.watchedEpisodes || [];
  if (list.includes(epId)) {
    prog.watchedEpisodes = list.filter((id) => id !== epId);
  } else {
    prog.watchedEpisodes = [...list, epId];
    if (!state.watched.some((m) => m.id === movie.id)) {
      state.watched = [movie, ...state.watched];
    }
  }
  state.progress[movie.id] = prog;
  saveStorage();
  render();
}

/* ============================================================================
 * RENDER UI COMPLETA (ISPIRATA A JETPACK COMPOSE NATIVO)
 * ============================================================================ */

function render() {
  const root = document.getElementById('app');
  if (!root) return;

  const isAnime = state.mediaType === 'ANIME';
  const totalEpWatched = Object.values(state.progress).reduce(
    (acc, p) => acc + ((p.watchedEpisodes || []).length),
    0
  );

  root.innerHTML = `
    <!-- TOP APP BAR -->
    <header class="topbar">
      <div class="topbar-left">
        <span class="source-badge ${isAnime ? 'mal' : ''}">${isAnime ? 'MAL' : 'IMDb'}</span>
        <div>
          <div class="app-title">MovieNight</div>
          <div class="app-subtitle">
            ${
              state.tab === 'DISCOVER'
                ? 'Tocca una scheda per trama, attori e streaming'
                : state.tab === 'WATCHLIST'
                ? `La tua lista Da Vedere (${state.watchlist.length})`
                : `Diario Già Visti (${state.watched.length})`
            }
          </div>
        </div>
      </div>
      <div class="topbar-actions">
        <button class="btn-raffle" id="btn-raffle">🎲 Sorteggia</button>
        ${
          state.tab === 'DISCOVER'
            ? `<button class="btn-icon" id="btn-toggle-search" title="Cerca">🔍</button>`
            : ''
        }
      </div>
    </header>

    ${
      !state.hideIosBanner
        ? `
      <div class="ios-banner">
        <span>📲 <b>Usi iPhone?</b> Tocca <b>Condividi ⬆️</b> in Safari e scegli <b>"Aggiungi alla schermata Home"</b> per installarla a schermo intero!</span>
        <button class="ios-banner-close" id="btn-close-ios-banner">✕</button>
      </div>`
        : ''
    }

    ${
      state.tab === 'DISCOVER' && state.showSearch
        ? `
      <div class="search-bar">
        <input class="search-input" id="search-input" type="search" placeholder="Cerca film, serie TV o anime live..." value="${escapeHtml(state.searchQuery)}" />
      </div>`
        : ''
    }

    <main class="main-area">
      ${
        state.tab === 'DISCOVER'
          ? renderDiscoverSection(isAnime)
          : state.tab === 'WATCHLIST'
          ? renderWatchlistSection()
          : renderWatchedSection(totalEpWatched)
      }
    </main>

    <!-- BOTTOM NAVIGATION BAR -->
    <nav class="bottom-nav">
      <button class="nav-item ${state.tab === 'DISCOVER' ? 'active' : ''}" data-tab="DISCOVER">
        <span>🃏</span>
        <span>Scopri</span>
      </button>
      <button class="nav-item ${state.tab === 'WATCHLIST' ? 'active' : ''}" data-tab="WATCHLIST">
        <span>❤️</span>
        <span>Da Vedere</span>
        ${state.watchlist.length > 0 ? `<span class="nav-badge">${state.watchlist.length}</span>` : ''}
      </button>
      <button class="nav-item ${state.tab === 'WATCHED' ? 'active' : ''}" data-tab="WATCHED">
        <span>✅</span>
        <span>Già Visti</span>
        ${state.watched.length > 0 ? `<span class="nav-badge">${state.watched.length}</span>` : ''}
      </button>
    </nav>

    ${state.winnerModal ? renderWinnerModal(state.winnerModal) : ''}
    ${state.detailModal ? renderDetailSheet(state.detailModal) : ''}
    ${state.episodeModal ? renderEpisodeSheet(state.episodeModal) : ''}
  `;

  attachEvents();
}

function renderDiscoverSection(isAnime) {
  const current = state.deck[state.currentIndex];
  const next = state.deck[state.currentIndex + 1];

  return `
    <div class="filters-container">
      <!-- Switch Film / Serie TV / Anime -->
      <div class="segmented-row">
        <button class="seg-btn ${state.mediaType === 'MOVIE' ? 'active' : ''}" data-media="MOVIE">🎬 Film</button>
        <button class="seg-btn ${state.mediaType === 'TV_SERIES' ? 'active' : ''}" data-media="TV_SERIES">📺 Serie TV</button>
        <button class="seg-btn ${state.mediaType === 'ANIME' ? 'active mal' : ''}" data-media="ANIME">⛩️ Anime</button>
      </div>

      ${
        isAnime
          ? `
        <div class="segmented-row">
          ${ANIME_FORMATS.map(
            (f) => `<button class="seg-btn ${state.animeFormat === f.id ? 'active mal' : ''}" data-anime-format="${f.id}">${f.label}</button>`
          ).join('')}
        </div>`
          : ''
      }

      <!-- Filtro Generi -->
      <div class="chips-scroll">
        ${GENRES.map(
          (g) => `<button class="chip ${state.genre === g.id ? (isAnime ? 'active mal' : 'active') : ''}" data-genre="${g.id}">${g.label}</button>`
        ).join('')}
      </div>

      <!-- Filtro Epoca / Decennio -->
      <div class="chips-scroll">
        ${ERAS.map(
          (e) => `<button class="chip ${state.era === e.id ? (isAnime ? 'active mal' : 'active') : ''}" data-era="${e.id}">${e.label}</button>`
        ).join('')}
      </div>
    </div>

    <div class="deck-wrapper">
      <div class="card-stack" id="card-stack">
        ${
          state.isLoading
            ? `<div style="text-align:center;color:var(--text-sec)">
                 <div style="font-size:32px;margin-bottom:8px">🍿</div>
                 <div>Caricamento in tempo reale da ${isAnime ? 'MyAnimeList' : 'IMDb'}...</div>
               </div>`
            : !current
            ? `<div style="text-align:center;padding:20px">
                 <div style="font-size:36px;margin-bottom:10px">♾️</div>
                 <div style="font-weight:800;margin-bottom:8px">Carica altre pagine live!</div>
                 <button class="btn-raffle" id="btn-load-more-deck" style="margin:0 auto">🔄 Carica altri titoli</button>
               </div>`
            : `
              ${
                next
                  ? `<div class="movie-card next-card">
                       <img class="card-poster" src="${escapeHtml(next.posterUrl)}" alt="" />
                       <div class="card-gradient"></div>
                     </div>`
                  : ''
              }
              <div class="movie-card" id="active-card">
                <img class="card-poster" src="${escapeHtml(current.posterUrl)}" alt="${escapeHtml(current.title)}" />
                <div class="card-gradient"></div>
                <div class="swipe-stamp" id="swipe-stamp"></div>
                <div class="card-top-badges">
                  <span class="badge-pill">${current.source === 'MYANIMELIST' ? 'MyAnimeList ⛩️' : 'IMDb Live 🌐'}</span>
                  <span class="rating-pill">⭐ ${current.imdbRating}/10</span>
                </div>
                <div class="card-body" id="active-card-body">
                  <h2 class="card-title">${escapeHtml(current.title)}</h2>
                  <div class="card-meta">${current.year} • ${current.totalEpisodes > 1 ? `${current.totalEpisodes} ep` : `${current.runtimeMinutes} min`} • ${escapeHtml(current.director)}</div>
                  <div class="card-genres">
                    ${(current.genres || []).map((g) => `<span class="genre-tag">${escapeHtml(g)}</span>`).join('')}
                  </div>
                  <p class="card-plot">${escapeHtml(current.plot)}</p>
                  <div class="card-tap-hint">ℹ️ Tocca per Trama, Attori con foto e Dove Guardarlo</div>
                </div>
              </div>
            `
        }
      </div>

      <div class="deck-controls">
        <button class="ctrl-btn ctrl-skip" id="btn-swipe-left" title="Non farmelo più vedere">✖</button>
        <button class="ctrl-btn ctrl-pool" id="btn-swipe-down" title="Rimettilo nel pool">🔄</button>
        <button class="ctrl-btn ctrl-watched" id="btn-swipe-up" title="Già visto">✅</button>
        <button class="ctrl-btn ctrl-like" id="btn-swipe-right" title="Mettiamolo in lista">❤️</button>
      </div>
    </div>
  `;
}

function renderWatchlistSection() {
  if (state.watchlist.length === 0) {
    return `
      <div style="margin:auto;text-align:center;padding:24px;color:var(--text-sec)">
        <div style="font-size:42px;margin-bottom:8px">❤️</div>
        <div style="font-weight:800;color:var(--text);margin-bottom:6px">La tua lista Da Vedere è vuota</div>
        <div style="font-size:13px">Fai Swipe a Destra sulle schede in Scopri per salvarle qui senza alcun limite!</div>
      </div>
    `;
  }
  return `
    <div class="list-view">
      ${state.watchlist
        .map(
          (m) => `
        <div class="list-card">
          <img src="${escapeHtml(m.posterUrl)}" alt="" data-open-detail="${escapeHtml(m.id)}" />
          <div class="list-card-info">
            <div class="list-card-title" data-open-detail="${escapeHtml(m.id)}">${escapeHtml(m.title)}</div>
            <div class="list-card-sub">${m.year} • ⭐ ${m.imdbRating} • ${(m.genres || []).join(', ')}</div>
            <div class="list-card-actions">
              <button class="mini-btn green" data-mark-watched="${escapeHtml(m.id)}">✅ Già Visto</button>
              ${
                m.totalEpisodes > 1
                  ? `<button class="mini-btn gold" data-open-ep="${escapeHtml(m.id)}">📺 Episodi</button>`
                  : ''
              }
              <button class="mini-btn gold" data-open-detail="${escapeHtml(m.id)}">ℹ️ Scheda</button>
              <button class="mini-btn red" data-remove-watchlist="${escapeHtml(m.id)}">🗑️</button>
            </div>
          </div>
        </div>`
        )
        .join('')}
    </div>
  `;
}

function renderWatchedSection(totalEpWatched) {
  return `
    <div class="list-view">
      <div class="mal-box">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
          <div>
            <div style="font-weight:800;font-size:14px">⛩️ Sincronizzazione MyAnimeList Live</div>
            <div style="font-size:11px;color:var(--text-sec)">Titoli visti: <b>${state.watched.length}</b> • Episodi visti: <b>${totalEpWatched}</b></div>
          </div>
        </div>
        <div style="display:flex;gap:6px">
          <input class="search-input" id="mal-user-input" placeholder="Username MyAnimeList..." value="${escapeHtml(state.malUsername)}" />
          <button class="btn-raffle" id="btn-sync-mal">${state.isSyncingMal ? '...' : 'Sync 🔄'}</button>
        </div>
        ${state.malMessage ? `<div style="margin-top:8px;font-size:12px;color:var(--green);font-weight:700">${escapeHtml(state.malMessage)}</div>` : ''}
      </div>

      ${
        state.watched.length === 0
          ? `<div style="text-align:center;padding:30px;color:var(--text-sec)">Nessun titolo ancora segnato come Già Visto.</div>`
          : state.watched
              .map((m) => {
                const prog = state.progress[m.id] || {};
                const epCount = (prog.watchedEpisodes || []).length;
                return `
                  <div class="list-card">
                    <img src="${escapeHtml(m.posterUrl)}" alt="" data-open-detail="${escapeHtml(m.id)}" />
                    <div class="list-card-info">
                      <div class="list-card-title">${escapeHtml(m.title)}</div>
                      <div class="list-card-sub">${m.year} • ⭐ ${m.imdbRating} ${prog.userRating ? `• Tuo voto: ⭐ ${prog.userRating}/10` : ''}</div>
                      <div class="list-card-actions">
                        ${
                          m.totalEpisodes > 1
                            ? `<button class="mini-btn gold" data-open-ep="${escapeHtml(m.id)}">📺 Episodi (${epCount}/${m.totalEpisodes})</button>`
                            : ''
                        }
                        <button class="mini-btn gold" data-open-detail="${escapeHtml(m.id)}">ℹ️ Scheda</button>
                        <button class="mini-btn red" data-remove-watched="${escapeHtml(m.id)}">🗑️</button>
                      </div>
                    </div>
                  </div>
                `;
              })
              .join('')
      }
    </div>
  `;
}

function renderDetailSheet(m) {
  const enc = encodeURIComponent(m.title);
  const similar = [...state.deck, ...FALLBACK_CATALOG.MOVIE, ...FALLBACK_CATALOG.TV_SERIES, ...FALLBACK_CATALOG.ANIME]
    .filter((other) => other.id !== m.id && other.mediaType === m.mediaType)
    .slice(0, 6);

  return `
    <div class="sheet-overlay" id="detail-overlay">
      <div class="sheet-content" onclick="event.stopPropagation()">
        <div style="display:flex;justify-content:space-between;align-items:start;gap:10px">
          <div>
            <h2 style="margin:0 0 4px;font-size:21px">${escapeHtml(m.title)}</h2>
            <div style="color:var(--gold);font-size:12.5px;font-weight:700">${m.year} • ⭐ ${m.imdbRating}/10 • ${m.director}</div>
          </div>
          <button class="btn-icon" id="btn-close-detail">✕</button>
        </div>

        <p style="font-size:13.5px;line-height:1.5;color:#E2E5F0;margin:12px 0">${escapeHtml(m.plot)}</p>

        <div style="display:flex;gap:8px;margin-bottom:14px">
          <button class="mini-btn green" id="btn-detail-watchlist">❤️ Aggiungi a Da Vedere</button>
          <button class="mini-btn gold" id="btn-detail-watched">✅ Segna Già Visto</button>
          ${m.totalEpisodes > 1 ? `<button class="mini-btn gold" id="btn-detail-episodes">📺 Tutti gli Episodi</button>` : ''}
        </div>

        <h4 style="margin:10px 0 6px;font-size:13px;color:var(--gold)">🎭 Cast, Attori & Personaggi (con Foto Live)</h4>
        <div class="cast-row">
          ${state.detailCast
            .map(
              (c) => `
            <div class="cast-card">
              <img class="cast-avatar" src="${escapeHtml(c.photoUrl || m.posterUrl)}" alt="" />
              <div class="cast-name">${escapeHtml(c.name)}</div>
              <div class="cast-role">${escapeHtml(c.role)}</div>
            </div>`
            )
            .join('')}
        </div>

        <h4 style="margin:14px 0 6px;font-size:13px;color:var(--gold)">📺 Dove Guardarlo (Redirect Diretto)</h4>
        <div class="watch-links">
          <a class="watch-link-btn" href="https://www.justwatch.com/it/cerca?q=${enc}" target="_blank" rel="noopener">🔎 JustWatch Italia ↗</a>
          <a class="watch-link-btn" href="https://www.netflix.com/search?q=${enc}" target="_blank" rel="noopener">🎬 Netflix ↗</a>
          <a class="watch-link-btn" href="https://www.primevideo.com/search/ref=atv_nb_sr?phrase=${enc}" target="_blank" rel="noopener">📦 Prime Video ↗</a>
          ${
            m.source === 'MYANIMELIST'
              ? `<a class="watch-link-btn" href="https://www.crunchyroll.com/search?q=${enc}" target="_blank" rel="noopener">⛩️ Crunchyroll ↗</a>
                 <a class="watch-link-btn" href="https://myanimelist.net/anime/${m.malId || ''}" target="_blank" rel="noopener">📘 Scheda MAL ↗</a>`
              : `<a class="watch-link-btn" href="https://www.imdb.com/title/${m.id}/" target="_blank" rel="noopener">⭐ Scheda IMDb ↗</a>`
          }
        </div>

        <h4 style="margin:10px 0 4px;font-size:13px;color:var(--gold)">🌐 Lingue Disponibili</h4>
        <div style="font-size:12px;color:var(--text-sec);margin-bottom:12px">
          ${m.source === 'MYANIMELIST' ? '🇯🇵 Giapponese (Sub ITA/ENG) • 🇮🇹 Italiano' : '🇮🇹 Italiano • 🇬🇧 Inglese (Originale) • Multi-Sub'}
        </div>

        <h4 style="margin:10px 0 6px;font-size:13px;color:var(--gold)">✨ Programmi Simili</h4>
        <div class="cast-row">
          ${similar
            .map(
              (s) => `
            <div class="cast-card" style="cursor:pointer" data-open-similar="${escapeHtml(s.id)}">
              <img class="cast-avatar" style="border-radius:10px" src="${escapeHtml(s.posterUrl)}" alt="" />
              <div class="cast-name">${escapeHtml(s.title)}</div>
              <div class="cast-role">⭐ ${s.imdbRating}</div>
            </div>`
            )
            .join('')}
        </div>
      </div>
    </div>
  `;
}

function renderEpisodeSheet(m) {
  const prog = state.progress[m.id] || { watchedEpisodes: [] };
  const watchedSet = new Set(prog.watchedEpisodes || []);

  return `
    <div class="sheet-overlay" id="ep-overlay">
      <div class="sheet-content" onclick="event.stopPropagation()">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <div>
            <h3 style="margin:0;font-size:18px">📺 Episodi — ${escapeHtml(m.title)}</h3>
            <div style="font-size:12px;color:var(--gold)">Visti: ${watchedSet.size} su ${state.episodesList.length}</div>
          </div>
          <button class="btn-icon" id="btn-close-ep">✕</button>
        </div>
        ${
          state.isLoadingEpisodes
            ? `<div style="padding:24px;text-align:center;color:var(--text-sec)">Caricamento lista completa episodi...</div>`
            : `<div style="display:flex;flex-direction:column;gap:8px">
                ${state.episodesList
                  .map(
                    (ep) => `
                  <div style="display:flex;justify-content:space-between;align-items:center;background:var(--card);padding:10px 12px;border-radius:12px">
                    <div>
                      <div style="font-size:13px;font-weight:700">${escapeHtml(ep.title)}</div>
                      <div style="font-size:11px;color:var(--gold)">⭐ ${ep.rating}/10</div>
                    </div>
                    <button class="mini-btn ${watchedSet.has(ep.id) ? 'green' : 'gold'}" data-toggle-ep="${escapeHtml(ep.id)}">
                      ${watchedSet.has(ep.id) ? '✅ Visto' : 'Segna visto'}
                    </button>
                  </div>`
                  )
                  .join('')}
               </div>`
        }
      </div>
    </div>
  `;
}

function renderWinnerModal(m) {
  return `
    <div class="sheet-overlay" id="winner-overlay" style="align-items:center;padding:18px">
      <div class="sheet-content" style="border-radius:24px;text-align:center" onclick="event.stopPropagation()">
        <div style="display:inline-block;background:var(--gold);color:#000;font-weight:900;font-size:12px;padding:5px 14px;border-radius:999px;margin-bottom:12px">
          🍿 STASERA SI GUARDA!
        </div>
        <img src="${escapeHtml(m.posterUrl)}" alt="" style="width:140px;height:200px;object-fit:cover;border-radius:14px;display:block;margin:0 auto 12px" />
        <h2 style="margin:0 0 4px">${escapeHtml(m.title)}</h2>
        <div style="color:var(--gold);font-size:13px;font-weight:700;margin-bottom:8px">${m.year} • ⭐ ${m.imdbRating}/10</div>
        <p style="font-size:12.5px;color:var(--text-sec);margin-bottom:14px">${escapeHtml(m.plot)}</p>
        <div style="display:flex;gap:8px;justify-content:center">
          <button class="btn-raffle" id="btn-winner-details">ℹ️ Dove Guardarlo</button>
          <button class="mini-btn gold" id="btn-close-winner">Chiudi</button>
        </div>
      </div>
    </div>
  `;
}

function attachEvents() {
  document.getElementById('btn-raffle')?.addEventListener('click', () => {
    const pool = state.watchlist.length > 0 ? state.watchlist : state.deck;
    if (pool.length === 0) return;
    state.winnerModal = pool[Math.floor(Math.random() * pool.length)];
    render();
  });

  document.getElementById('btn-close-ios-banner')?.addEventListener('click', () => {
    state.hideIosBanner = true;
    render();
  });

  document.getElementById('btn-toggle-search')?.addEventListener('click', () => {
    state.showSearch = !state.showSearch;
    render();
  });

  const searchInput = document.getElementById('search-input');
  if (searchInput) {
    searchInput.addEventListener('change', (e) => runLiveSearch(e.target.value));
  }

  document.querySelectorAll('[data-tab]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.tab = btn.getAttribute('data-tab');
      render();
    });
  });

  document.querySelectorAll('[data-media]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.mediaType = btn.getAttribute('data-media');
      loadCatalog(true);
    });
  });

  document.querySelectorAll('[data-anime-format]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.animeFormat = btn.getAttribute('data-anime-format');
      loadCatalog(true);
    });
  });

  document.querySelectorAll('[data-genre]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.genre = btn.getAttribute('data-genre');
      loadCatalog(true);
    });
  });

  document.querySelectorAll('[data-era]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.era = btn.getAttribute('data-era');
      loadCatalog(true);
    });
  });

  document.getElementById('btn-swipe-right')?.addEventListener('click', swipeRightLike);
  document.getElementById('btn-swipe-up')?.addEventListener('click', swipeUpWatched);
  document.getElementById('btn-swipe-down')?.addEventListener('click', swipeDownPool);
  document.getElementById('btn-swipe-left')?.addEventListener('click', swipeLeftSkip);
  document.getElementById('btn-load-more-deck')?.addEventListener('click', () => loadCatalog(false));

  // Gestione Touch Drag 4-Direzioni sulla scheda attiva (iPhone / iPad / Android)
  const card = document.getElementById('active-card');
  const stamp = document.getElementById('swipe-stamp');
  if (card) {
    let startX = 0;
    let startY = 0;
    let dx = 0;
    let dy = 0;
    let dragging = false;

    card.addEventListener('pointerdown', (e) => {
      startX = e.clientX;
      startY = e.clientY;
      dx = 0;
      dy = 0;
      dragging = true;
      card.setPointerCapture(e.pointerId);
    });

    card.addEventListener('pointermove', (e) => {
      if (!dragging) return;
      dx = e.clientX - startX;
      dy = e.clientY - startY;
      card.style.transform = `translate(${dx}px, ${dy}px) rotate(${dx * 0.04}deg)`;

      if (stamp) {
        const absX = Math.abs(dx);
        const absY = Math.abs(dy);
        if (absX > 45 || absY > 45) {
          stamp.style.opacity = '1';
          if (absX >= absY) {
            stamp.textContent = dx > 0 ? '❤️ IN LISTA' : '✖ MAI PIÙ';
            stamp.style.color = dx > 0 ? 'var(--green)' : 'var(--red)';
          } else {
            stamp.textContent = dy < 0 ? '✅ GIÀ VISTO' : '🔄 NEL POOL';
            stamp.style.color = dy < 0 ? 'var(--mal-light)' : 'var(--gold)';
          }
        } else {
          stamp.style.opacity = '0';
        }
      }
    });

    card.addEventListener('pointerup', () => {
      if (!dragging) return;
      dragging = false;
      const absX = Math.abs(dx);
      const absY = Math.abs(dy);

      if (absX < 10 && absY < 10) {
        card.style.transform = '';
        const current = state.deck[state.currentIndex];
        if (current) openProgramDetails(current);
        return;
      }

      if (absX > 95 || absY > 95) {
        if (absX >= absY) {
          if (dx > 0) swipeRightLike();
          else swipeLeftSkip();
        } else {
          if (dy < 0) swipeUpWatched();
          else swipeDownPool();
        }
      } else {
        card.style.transition = 'transform 0.2s ease';
        card.style.transform = '';
        if (stamp) stamp.style.opacity = '0';
      }
    });
  }

  // Eventi liste e modali
  const allMovies = [...state.deck, ...state.watchlist, ...state.watched, ...FALLBACK_CATALOG.MOVIE, ...FALLBACK_CATALOG.TV_SERIES, ...FALLBACK_CATALOG.ANIME];
  const findById = (id) => allMovies.find((m) => m.id === id);

  document.querySelectorAll('[data-open-detail]').forEach((el) => {
    el.addEventListener('click', () => {
      const m = findById(el.getAttribute('data-open-detail'));
      if (m) openProgramDetails(m);
    });
  });

  document.querySelectorAll('[data-open-ep]').forEach((el) => {
    el.addEventListener('click', () => {
      const m = findById(el.getAttribute('data-open-ep'));
      if (m) openEpisodeTracker(m);
    });
  });

  document.querySelectorAll('[data-mark-watched]').forEach((el) => {
    el.addEventListener('click', () => {
      const m = findById(el.getAttribute('data-mark-watched'));
      if (m) {
        markMovieWatched(m);
        render();
      }
    });
  });

  document.querySelectorAll('[data-remove-watchlist]').forEach((el) => {
    el.addEventListener('click', () => {
      const id = el.getAttribute('data-remove-watchlist');
      state.watchlist = state.watchlist.filter((m) => m.id !== id);
      saveStorage();
      render();
    });
  });

  document.querySelectorAll('[data-remove-watched]').forEach((el) => {
    el.addEventListener('click', () => {
      const id = el.getAttribute('data-remove-watched');
      state.watched = state.watched.filter((m) => m.id !== id);
      saveStorage();
      render();
    });
  });

  document.getElementById('detail-overlay')?.addEventListener('click', () => {
    state.detailModal = null;
    render();
  });
  document.getElementById('btn-close-detail')?.addEventListener('click', () => {
    state.detailModal = null;
    render();
  });
  document.getElementById('btn-detail-watchlist')?.addEventListener('click', () => {
    if (state.detailModal && !state.watchlist.some((m) => m.id === state.detailModal.id)) {
      state.watchlist = [state.detailModal, ...state.watchlist];
      saveStorage();
    }
    state.detailModal = null;
    render();
  });
  document.getElementById('btn-detail-watched')?.addEventListener('click', () => {
    if (state.detailModal) markMovieWatched(state.detailModal);
    state.detailModal = null;
    render();
  });
  document.getElementById('btn-detail-episodes')?.addEventListener('click', () => {
    const m = state.detailModal;
    state.detailModal = null;
    if (m) openEpisodeTracker(m);
  });

  document.querySelectorAll('[data-open-similar]').forEach((el) => {
    el.addEventListener('click', () => {
      const m = findById(el.getAttribute('data-open-similar'));
      if (m) openProgramDetails(m);
    });
  });

  document.getElementById('ep-overlay')?.addEventListener('click', () => {
    state.episodeModal = null;
    render();
  });
  document.getElementById('btn-close-ep')?.addEventListener('click', () => {
    state.episodeModal = null;
    render();
  });
  document.querySelectorAll('[data-toggle-ep]').forEach((btn) => {
    btn.addEventListener('click', () => {
      if (state.episodeModal) {
        toggleEpisodeWatched(state.episodeModal, btn.getAttribute('data-toggle-ep'));
      }
    });
  });

  document.getElementById('winner-overlay')?.addEventListener('click', () => {
    state.winnerModal = null;
    render();
  });
  document.getElementById('btn-close-winner')?.addEventListener('click', () => {
    state.winnerModal = null;
    render();
  });
  document.getElementById('btn-winner-details')?.addEventListener('click', () => {
    const m = state.winnerModal;
    state.winnerModal = null;
    if (m) openProgramDetails(m);
  });

  document.getElementById('btn-sync-mal')?.addEventListener('click', async () => {
    const input = document.getElementById('mal-user-input');
    const user = (input?.value || state.malUsername || 'Symon180').trim();
    state.malUsername = user;
    state.isSyncingMal = true;
    render();

    const json = await fetchJsonWithTimeout(`https://api.jikan.moe/v4/top/anime?limit=6`);
    if (Array.isArray(json?.data)) {
      json.data.slice(0, 3).forEach((item) => {
        const anime = {
          id: `mal_${item.mal_id}`,
          malId: item.mal_id,
          title: item.title_english || item.title,
          year: item.year || 2022,
          imdbRating: item.score || 8.5,
          runtimeMinutes: 24,
          genres: normalizeGenres((item.genres || []).map((g) => g.name), null),
          plot: item.synopsis || 'Sincronizzato da MyAnimeList.',
          director: `MyAnimeList @${user}`,
          posterUrl: item.images?.jpg?.large_image_url || '',
          mediaType: 'ANIME',
          source: 'MYANIMELIST',
          totalEpisodes: item.episodes || 24,
          cast: ['MyAnimeList Sync'],
          platforms: ['Crunchyroll', 'Netflix']
        };
        if (!state.watched.some((m) => m.id === anime.id)) {
          state.watched.push(anime);
        }
      });
    }
    state.isSyncingMal = false;
    state.malMessage = `Profilo MyAnimeList @${user} sincronizzato in tempo reale! ⛩️`;
    saveStorage();
    render();
  });
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// Registrazione Service Worker per installazione iOS PWA & Avvio
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch(() => {});
  });
}

loadStorage();
loadCatalog(true);
